import { useEffect, useState } from 'react';
import { CustomerApi } from '../api';

const EMPTY_FORM = { name: '', email: '', phone: '', address: '' };

export default function CustomerSection() {
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
      <h2>Customers</h2>

      {error && <div className="message">{error}</div>}

      <form className="form-grid" onSubmit={handleSubmit}>
        <div className="field">
          <label>Name</label>
          <input value={form.name} onChange={handleChange('name')} required />
        </div>
        <div className="field">
          <label>Email</label>
          <input type="email" value={form.email} onChange={handleChange('email')} required />
        </div>
        <div className="field">
          <label>Phone</label>
          <input value={form.phone} onChange={handleChange('phone')} required />
        </div>
        <div className="field">
          <label>Address</label>
          <input value={form.address} onChange={handleChange('address')} required />
        </div>
        <div className="row-actions">
          <button type="submit" className="primary" disabled={loading}>
            {loading ? 'Saving...' : editingId ? 'Update Customer' : 'Add Customer'}
          </button>
          {editingId && (
            <button type="button" onClick={handleCancelEdit}>
              Cancel
            </button>
          )}
        </div>
      </form>

      {customers.length === 0 ? (
        <p className="empty">No customers yet.</p>
      ) : (
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>Name</th>
              <th>Email</th>
              <th>Phone</th>
              <th>Address</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {customers.map((c) => (
              <tr key={c.id}>
                <td>{c.id}</td>
                <td>{c.name}</td>
                <td>{c.email}</td>
                <td>{c.phone}</td>
                <td>{c.address}</td>
                <td>
                  <div className="row-actions">
                    <button onClick={() => handleEdit(c)}>Edit</button>
                    <button onClick={() => handleDelete(c.id)}>Delete</button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
