import { useEffect, useState } from 'react';
import { BeneficiaryApi } from '../api';
import { can } from '../permissions';
import { Alert, Button, Card, Empty, Field, FormActions, FormGrid, PageHeader, Table, Td, inputClass } from './ui';

const EMPTY_FORM = { name: '', accountNumber: '', bankName: '', ifscCode: '', customerId: '' };

export default function BeneficiarySection() {
  const canDelete = can('deleteBeneficiary');
  const [beneficiaries, setBeneficiaries] = useState([]);
  const [form, setForm] = useState(EMPTY_FORM);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const load = async () => {
    setError('');
    try {
      setBeneficiaries(await BeneficiaryApi.list());
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
      await BeneficiaryApi.create({ ...form, customerId: Number(form.customerId) });
      setForm(EMPTY_FORM);
      await load();
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (id) => {
    setError('');
    try {
      await BeneficiaryApi.remove(id);
      await load();
    } catch (err) {
      setError(err.message);
    }
  };

  return (
    <div>
      <PageHeader title="Beneficiaries" description="Saved payees a customer can send money to." />

      <Alert>{error}</Alert>

      <Card title="Add beneficiary">
        <FormGrid onSubmit={handleSubmit}>
          <Field label="Name">
            <input className={inputClass} value={form.name} onChange={handleChange('name')} required />
          </Field>
          <Field label="Account Number">
            <input className={inputClass} value={form.accountNumber} onChange={handleChange('accountNumber')} required />
          </Field>
          <Field label="Bank Name">
            <input className={inputClass} value={form.bankName} onChange={handleChange('bankName')} required />
          </Field>
          <Field label="IFSC Code">
            <input className={inputClass} value={form.ifscCode} onChange={handleChange('ifscCode')} placeholder="ABCD0123456" required />
          </Field>
          <Field label="Customer ID">
            <input className={inputClass} type="number" value={form.customerId} onChange={handleChange('customerId')} required />
          </Field>
          <FormActions>
            <Button type="submit" variant="primary" disabled={loading}>
              {loading ? 'Adding...' : 'Add Beneficiary'}
            </Button>
          </FormActions>
        </FormGrid>
      </Card>

      <Card title={`All beneficiaries (${beneficiaries.length})`}>
        {beneficiaries.length === 0 ? (
          <Empty>No beneficiaries yet.</Empty>
        ) : (
          <Table columns={['ID', 'Name', 'Account Number', 'Bank', 'IFSC', ...(canDelete ? [''] : [])]}>
            {beneficiaries.map((b) => (
              <tr key={b.id} className="hover:bg-wash/60">
                <Td className="text-muted">{b.id}</Td>
                <Td className="font-bold">{b.name}</Td>
                <Td className="tabular-nums">{b.accountNumber}</Td>
                <Td>{b.bankName}</Td>
                <Td className="tabular-nums">{b.ifscCode}</Td>
                {canDelete && (
                  <Td className="text-right">
                    <Button size="sm" variant="ghost" onClick={() => handleDelete(b.id)}>Delete</Button>
                  </Td>
                )}
              </tr>
            ))}
          </Table>
        )}
      </Card>
    </div>
  );
}
