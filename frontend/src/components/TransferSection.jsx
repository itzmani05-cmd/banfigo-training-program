import { useEffect, useState } from 'react';
import { AccountApi, BeneficiaryApi, TransferApi } from '../api';

const EMPTY_FORM = { fromAccountId: '', toAccountId: '', beneficiaryId: '', amount: '', description: '' };

const accountLabel = (a) => `${a.accountNumber} - ${a.customerName || `Customer ${a.customerId}`} (balance ${a.balance})`;

export default function TransferSection() {
  const [mode, setMode] = useState('account');
  const [accounts, setAccounts] = useState([]);
  const [beneficiaries, setBeneficiaries] = useState([]);
  const [form, setForm] = useState(EMPTY_FORM);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState(null);

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
    const next = { ...form, [field]: e.target.value };
    // A beneficiary picked for the previous source account may not belong to the new one
    if (field === 'fromAccountId') next.beneficiaryId = '';
    setForm(next);
  };

  const switchMode = (next) => {
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
    try {
      const transfer = await TransferApi.create({
        fromAccountId: Number(form.fromAccountId),
        ...(mode === 'account'
          ? { toAccountId: Number(form.toAccountId) }
          : { beneficiaryId: Number(form.beneficiaryId) }),
        amount: Number(form.amount),
        description: form.description,
      });
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
      <div className="section-title">
        <h2>Transfers</h2>
      </div>

      {error && <div className="message">{error}</div>}

      <div className="row-actions" style={{ marginBottom: 16 }}>
        <button className={mode === 'account' ? 'active' : ''} onClick={() => switchMode('account')}>
          To account
        </button>
        <button className={mode === 'beneficiary' ? 'active' : ''} onClick={() => switchMode('beneficiary')}>
          Pay beneficiary
        </button>
      </div>

      <form className="form-grid" onSubmit={handleSubmit}>
        <div className="field">
          <label>From Account</label>
          <select value={form.fromAccountId} onChange={handleChange('fromAccountId')} required>
            <option value="">Select account</option>
            {accounts.map((a) => (
              <option key={a.id} value={a.id}>{accountLabel(a)}</option>
            ))}
          </select>
        </div>

        {mode === 'account' ? (
          <div className="field">
            <label>To Account</label>
            <select value={form.toAccountId} onChange={handleChange('toAccountId')} required>
              <option value="">Select account</option>
              {accounts
                .filter((a) => String(a.id) !== form.fromAccountId)
                .map((a) => (
                  <option key={a.id} value={a.id}>{accountLabel(a)}</option>
                ))}
            </select>
          </div>
        ) : (
          <div className="field">
            <label>Beneficiary</label>
            <select
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
          </div>
        )}

        <div className="field">
          <label>Amount</label>
          <input type="number" min="0.01" step="0.01" value={form.amount} onChange={handleChange('amount')} required />
        </div>
        <div className="field">
          <label>Description</label>
          <input value={form.description} onChange={handleChange('description')} maxLength={200} />
        </div>
        <button type="submit" className="primary" disabled={loading}>
          {loading ? 'Transferring...' : 'Transfer'}
        </button>
      </form>

      {result && (
        <table>
          <tbody>
            <tr>
              <th>Reference</th>
              <td>{result.reference}</td>
            </tr>
            <tr>
              <th>From Account</th>
              <td>{result.fromAccountId}</td>
            </tr>
            <tr>
              <th>To</th>
              <td>
                {result.beneficiaryName
                  ? `${result.beneficiaryName} (A/C ${result.toAccountNumber})`
                  : `A/C ${result.toAccountNumber}`}
              </td>
            </tr>
            <tr>
              <th>Amount</th>
              <td>{result.amount}</td>
            </tr>
            <tr>
              <th>Source Balance After</th>
              <td>{result.fromAccountBalance}</td>
            </tr>
            <tr>
              <th>Date</th>
              <td>{result.transferDate}</td>
            </tr>
          </tbody>
        </table>
      )}
    </div>
  );
}
