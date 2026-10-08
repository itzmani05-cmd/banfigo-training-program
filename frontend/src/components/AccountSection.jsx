import { useEffect, useState } from 'react';
import { AccountApi } from '../api';
import { money } from '../format';
import AccountDetails from './AccountDetails';
import { can, isCustomer, rolesFor } from '../permissions';
import { Alert, Badge, Button, Card, Empty, Field, FormActions, FormGrid, PageHeader, Table, Td, ViewOnly, inputClass } from './ui';

const EMPTY_FORM = { accountNumber: '', accountType: '', customerId: '' };

export default function AccountSection() {
  const [accounts, setAccounts] = useState([]);
  const [form, setForm] = useState(EMPTY_FORM);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [selectedId, setSelectedId] = useState(null);

  const load = async () => {
    setError('');
    try {
      setAccounts(await AccountApi.list());
    } catch (err) {
      setError(err.message);
    }
  };

  useEffect(() => {
    load();
  }, []);

  const handleChange = (field) => (e) => setForm({ ...form, [field]: e.target.value });

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await AccountApi.create({ ...form, customerId: Number(form.customerId) });
      setForm(EMPTY_FORM);
      await load();
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleBack = async () => {
    setSelectedId(null);
    await load();
  };

  if (selectedId) {
    return <AccountDetails accountId={selectedId} onBack={handleBack} />;
  }

  return (
    <div>
      <PageHeader
        title="Bank Accounts"
        description={isCustomer() ? 'Your accounts, balances and statements.' : 'Open new accounts and view balances and history.'}
      />

      <Alert>{error}</Alert>

      {can('createAccount') ? (
        <Card title="Open account">
          <FormGrid onSubmit={handleSubmit}>
            <Field label="Account Number" hint="10 digits">
              <input className={inputClass} value={form.accountNumber} onChange={handleChange('accountNumber')} required />
            </Field>
            <Field label="Account Type">
              <input
                className={inputClass}
                value={form.accountType}
                onChange={handleChange('accountType')}
                placeholder="SAVINGS / CURRENT"
                required
              />
            </Field>
            <Field label="Customer ID">
              <input className={inputClass} type="number" value={form.customerId} onChange={handleChange('customerId')} required />
            </Field>
            <FormActions>
              <Button type="submit" variant="primary" disabled={loading}>
                {loading ? 'Creating...' : 'Create Account'}
              </Button>
            </FormActions>
          </FormGrid>
        </Card>
      ) : (
        <ViewOnly>
          {isCustomer()
            ? 'To open a new account, contact the bank.'
            : `Opening accounts requires the ${rolesFor('createAccount')} role.`}
        </ViewOnly>
      )}

      <Card title={`${isCustomer() ? 'Your' : 'All'} accounts (${accounts.length})`}>
        {accounts.length === 0 ? (
          <Empty>No accounts yet.</Empty>
        ) : (
          <Table columns={['ID', 'Account Number', 'Type', 'Customer', '>Balance', '']}>
            {accounts.map((a) => (
              <tr key={a.id} className="hover:bg-wash/60">
                <Td className="text-muted">{a.id}</Td>
                <Td className="font-bold tabular-nums">{a.accountNumber}</Td>
                <Td><Badge>{a.accountType}</Badge></Td>
                <Td>{a.customerName || `Customer ${a.customerId}`}</Td>
                <Td className="text-right font-bold tabular-nums">{money(a.balance)}</Td>
                <Td className="text-right">
                  <Button size="sm" onClick={() => setSelectedId(a.id)}>View</Button>
                </Td>
              </tr>
            ))}
          </Table>
        )}
      </Card>
    </div>
  );
}
