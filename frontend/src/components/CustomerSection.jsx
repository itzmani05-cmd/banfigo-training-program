import { useEffect, useState } from 'react';
import { CustomerApi } from '../api';
import { can, rolesFor } from '../permissions';
import { Alert, Button, Card, Empty, Field, FormActions, FormGrid, PageHeader, Table, Td, ViewOnly, inputClass } from './ui';

const EMPTY_FORM = { name: '', email: '', phone: '', address: '' };

export default function CustomerSection() {
  const canManage = can('manageCustomers');
  const [customers, setCustomers] = useState([]);
  const [form, setForm] = useState(EMPTY_FORM);
  const [editingId, setEditingId] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const load = async () => {
    setError('');
    try {
      setCustomers(await CustomerApi.list());
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
      if (editingId) {
        await CustomerApi.update(editingId, form);
      } else {
        await CustomerApi.create(form);
      }
      setForm(EMPTY_FORM);
      setEditingId(null);
      await load();
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleEdit = (customer) => {
    setEditingId(customer.id);
    setForm({
      name: customer.name,
      email: customer.email,
      phone: customer.phone,
      address: customer.address,
    });
  };

  const handleCancelEdit = () => {
    setEditingId(null);
    setForm(EMPTY_FORM);
  };

  const handleDelete = async (id) => {
    setError('');
    try {
      await CustomerApi.remove(id);
      if (editingId === id) handleCancelEdit();
      await load();
    } catch (err) {
      setError(err.message);
    }
  };

  return (
    <div>
      <PageHeader title="Customers" description="People who hold accounts with the bank." />

      <Alert>{error}</Alert>

      {!canManage && (
        <ViewOnly>You can view customers. Adding, editing or deleting them requires the {rolesFor('manageCustomers')} role.</ViewOnly>
      )}

      {canManage && (
        <Card title={editingId ? `Edit customer #${editingId}` : 'Add customer'}>
          <FormGrid onSubmit={handleSubmit}>
            <Field label="Name">
              <input className={inputClass} value={form.name} onChange={handleChange('name')} required />
            </Field>
            <Field label="Email">
              <input className={inputClass} type="email" value={form.email} onChange={handleChange('email')} required />
            </Field>
            <Field label="Phone">
              <input className={inputClass} value={form.phone} onChange={handleChange('phone')} required />
            </Field>
            <Field label="Address">
              <input className={inputClass} value={form.address} onChange={handleChange('address')} required />
            </Field>
            <FormActions>
              <Button type="submit" variant="primary" disabled={loading}>
                {loading ? 'Saving...' : editingId ? 'Update Customer' : 'Add Customer'}
              </Button>
              {editingId && <Button onClick={handleCancelEdit}>Cancel</Button>}
            </FormActions>
          </FormGrid>
        </Card>
      )}

      <Card title={`All customers (${customers.length})`}>
        {customers.length === 0 ? (
          <Empty>No customers yet.</Empty>
        ) : (
          <Table columns={['ID', 'Name', 'Email', 'Phone', 'Address', ...(canManage ? [''] : [])]}>
            {customers.map((c) => (
              <tr key={c.id} className={editingId === c.id ? 'bg-wash' : 'hover:bg-wash/60'}>
                <Td className="text-muted">{c.id}</Td>
                <Td className="font-bold">{c.name}</Td>
                <Td>{c.email}</Td>
                <Td>{c.phone}</Td>
                <Td>{c.address}</Td>
                {canManage && (
                  <Td>
                    <div className="flex justify-end gap-2">
                      <Button size="sm" onClick={() => handleEdit(c)}>Edit</Button>
                      <Button size="sm" variant="ghost" onClick={() => handleDelete(c.id)}>Delete</Button>
                    </div>
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
