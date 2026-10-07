import { useState } from 'react';
import { TransactionApi } from '../api';
import { money } from '../format';
import { can, rolesFor } from '../permissions';
import {
  Alert, Button, Card, Empty, Field, FormActions, FormGrid, PageHeader, Table, Td, TransactionTypeBadge, ViewOnly, inputClass,
} from './ui';

const EMPTY_FORM = { transactionType: 'DEPOSIT', amount: '', description: '' };

export default function TransactionSection() {
  const [accountId, setAccountId] = useState('');
  const [transactions, setTransactions] = useState([]);
  const [form, setForm] = useState(EMPTY_FORM);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [loaded, setLoaded] = useState(false);

  const load = async () => {
    if (!accountId) {
      setError('Enter an account ID first.');
      return;
    }
    setError('');
    try {
      const page = await TransactionApi.listByAccount(accountId, { size: 50 });
      setTransactions(page.content);
      setLoaded(true);
    } catch (err) {
      setError(err.message);
    }
  };

  const handleChange = (field) => (e) => setForm({ ...form, [field]: e.target.value });

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!accountId) {
      setError('Enter an account ID first.');
      return;
    }
    setError('');
    setLoading(true);
    try {
      await TransactionApi.create(accountId, { ...form, amount: Number(form.amount) });
      setForm(EMPTY_FORM);
      await load();
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <PageHeader title="Transactions" description="Record deposits and withdrawals on an account." />

      <Alert>{error}</Alert>

      <Card title="Account">
        <div className="flex max-w-md items-end gap-2">
          <Field label="Account ID" className="flex-1">
            <input className={inputClass} type="number" value={accountId} onChange={(e) => setAccountId(e.target.value)} />
          </Field>
          <Button onClick={load}>Load</Button>
        </div>
      </Card>

      {can('createTransaction') ? (
        <Card title="New transaction">
          <FormGrid onSubmit={handleSubmit}>
            <Field label="Type">
              <select className={inputClass} value={form.transactionType} onChange={handleChange('transactionType')}>
                <option value="DEPOSIT">DEPOSIT</option>
                <option value="WITHDRAWAL">WITHDRAWAL</option>
              </select>
            </Field>
            <Field label="Amount">
              <input className={inputClass} type="number" min="1" value={form.amount} onChange={handleChange('amount')} required />
            </Field>
            <Field label="Description" className="sm:col-span-2">
              <input className={inputClass} value={form.description} onChange={handleChange('description')} />
            </Field>
            <FormActions>
              <Button type="submit" variant="primary" disabled={loading}>
                {loading ? 'Submitting...' : 'Submit Transaction'}
              </Button>
            </FormActions>
          </FormGrid>
        </Card>
      ) : (
        <ViewOnly>You can view transactions. Recording deposits or withdrawals requires the {rolesFor('createTransaction')} role.</ViewOnly>
      )}

      {loaded && (
        <Card title={`Transactions for account ${accountId}`}>
          {transactions.length === 0 ? (
            <Empty>No transactions for this account.</Empty>
          ) : (
            <Table columns={['ID', 'Type', 'Description', 'Date', '>Amount']}>
              {transactions.map((t) => (
                <tr key={t.id} className="hover:bg-wash/60">
                  <Td className="text-muted">{t.id}</Td>
                  <Td><TransactionTypeBadge type={t.transactionType} /></Td>
                  <Td>{t.description || '-'}</Td>
                  <Td className="whitespace-nowrap text-muted">{t.transactionDate.replace('T', ' ').slice(0, 16)}</Td>
                  <Td className="text-right font-bold tabular-nums">{money(t.amount)}</Td>
                </tr>
              ))}
            </Table>
          )}
        </Card>
      )}
    </div>
  );
}
