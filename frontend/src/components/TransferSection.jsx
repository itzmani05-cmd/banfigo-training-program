import { useEffect, useRef, useState } from 'react';
import { AccountApi, BeneficiaryApi, TransferApi } from '../api';
import { money } from '../format';
import { can, rolesFor } from '../permissions';
import { Alert, Button, Card, DetailList, Field, FormActions, FormGrid, PageHeader, Tabs, ViewOnly, inputClass } from './ui';

const EMPTY_FORM = { fromAccountId: '', toAccountId: '', beneficiaryId: '', amount: '', description: '' };

// crypto.randomUUID only exists on https or localhost
const newKey = () => crypto.randomUUID?.() ?? `${Date.now()}-${Math.random().toString(36).slice(2)}`;

const accountLabel = (a) => `${a.accountNumber} - ${a.customerName || `Customer ${a.customerId}`} (balance ${a.balance})`;

export default function TransferSection() {
  const [mode, setMode] = useState('account');
  const [accounts, setAccounts] = useState([]);
  const [beneficiaries, setBeneficiaries] = useState([]);
  const [form, setForm] = useState(EMPTY_FORM);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState(null);
  // One key per transfer attempt: kept if the request fails so "Transfer" again can't pay twice,
  // replaced once it succeeds or the form changes
  const idempotencyKey = useRef(null);

  const loadOptions = async () => {
    try {
      const [accountList, beneficiaryList] = await Promise.all([AccountApi.list(), BeneficiaryApi.list()]);
      setAccounts(accountList);
      setBeneficiaries(beneficiaryList);
    } catch (err) {
      setError(err.message);
    }
  };

  useEffect(() => {
    loadOptions();
  }, []);

  const fromAccount = accounts.find((a) => String(a.id) === form.fromAccountId);
  // Customers can only pay beneficiaries they saved themselves
  const ownBeneficiaries = fromAccount
    ? beneficiaries.filter((b) => b.customerId === fromAccount.customerId)
    : [];

  const handleChange = (field) => (e) => {
    idempotencyKey.current = null;
    const next = { ...form, [field]: e.target.value };
    // A beneficiary picked for the previous source account may not belong to the new one
    if (field === 'fromAccountId') next.beneficiaryId = '';
    setForm(next);
  };

  const switchMode = (next) => {
    idempotencyKey.current = null;
    setMode(next);
    setForm({ ...form, toAccountId: '', beneficiaryId: '' });
    setError('');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (mode === 'account' && form.fromAccountId === form.toAccountId) {
      setError('Source and destination accounts must be different.');
      return;
    }
    setError('');
    setResult(null);
    setLoading(true);
    idempotencyKey.current ??= newKey();
    try {
      const transfer = await TransferApi.create({
        fromAccountId: Number(form.fromAccountId),
        ...(mode === 'account'
          ? { toAccountId: Number(form.toAccountId) }
          : { beneficiaryId: Number(form.beneficiaryId) }),
        amount: Number(form.amount),
        description: form.description,
      }, idempotencyKey.current);
      idempotencyKey.current = null;
      setResult(transfer);
      setForm(EMPTY_FORM);
      await loadOptions(); // refresh balances shown in the dropdowns
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <PageHeader title="Transfers" description="Move money between accounts or pay a saved beneficiary." />

      <Alert>{error}</Alert>

      {result && (
        <Card title="Transfer complete" actions={<span className="text-label text-muted">Ref {result.reference}</span>}>
          <DetailList
            items={[
              ['Amount', money(result.amount)],
              ['From Account', result.fromAccountId],
              ['To', result.beneficiaryName
                ? `${result.beneficiaryName} (A/C ${result.toAccountNumber})`
                : `A/C ${result.toAccountNumber}`],
              ['Source Balance After', money(result.fromAccountBalance)],
              ['Date', result.transferDate.replace('T', ' ').slice(0, 16)],
            ]}
          />
        </Card>
      )}

      {!can('createTransfer') ? (
        <ViewOnly>Making transfers requires the {rolesFor('createTransfer')} role.</ViewOnly>
      ) : (
        <Card
          title="New transfer"
          actions={
            <Tabs
              value={mode}
              onChange={switchMode}
              options={[
                { value: 'account', label: 'To account' },
                { value: 'beneficiary', label: 'Pay beneficiary' },
              ]}
            />
          }
        >
          <FormGrid onSubmit={handleSubmit}>
            <Field label="From Account">
              <select className={inputClass} value={form.fromAccountId} onChange={handleChange('fromAccountId')} required>
                <option value="">Select account</option>
                {accounts.map((a) => (
                  <option key={a.id} value={a.id}>{accountLabel(a)}</option>
                ))}
              </select>
            </Field>

            {mode === 'account' ? (
              <Field label="To Account">
                <select className={inputClass} value={form.toAccountId} onChange={handleChange('toAccountId')} required>
                  <option value="">Select account</option>
                  {accounts
                    .filter((a) => String(a.id) !== form.fromAccountId)
                    .map((a) => (
                      <option key={a.id} value={a.id}>{accountLabel(a)}</option>
                    ))}
                </select>
              </Field>
            ) : (
              <Field label="Beneficiary">
                <select
                  className={inputClass}
                  value={form.beneficiaryId}
                  onChange={handleChange('beneficiaryId')}
                  required
                  disabled={!fromAccount}
                >
                  <option value="">
                    {!fromAccount
                      ? 'Select a source account first'
                      : ownBeneficiaries.length === 0
                        ? 'No beneficiaries saved for this customer'
                        : 'Select beneficiary'}
                  </option>
                  {ownBeneficiaries.map((b) => (
                    <option key={b.id} value={b.id}>
                      {b.name} - {b.bankName} A/C {b.accountNumber}
                    </option>
                  ))}
                </select>
              </Field>
            )}

            <Field label="Amount">
              <input
                className={inputClass}
                type="number"
                min="0.01"
                step="0.01"
                value={form.amount}
                onChange={handleChange('amount')}
                required
              />
            </Field>
            <Field label="Description">
              <input className={inputClass} value={form.description} onChange={handleChange('description')} maxLength={200} />
            </Field>
            <FormActions>
              <Button type="submit" variant="primary" disabled={loading}>
                {loading ? 'Transferring...' : 'Transfer'}
              </Button>
            </FormActions>
          </FormGrid>
        </Card>
      )}
    </div>
  );
}
