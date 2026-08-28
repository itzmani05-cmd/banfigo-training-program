import { useEffect, useState } from 'react';
import { BeneficiaryApi } from '../api';

const EMPTY_FORM = { name: '', accountNumber: '', bankName: '', ifscCode: '', customerId: '' };

export default function BeneficiarySection() {
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
      <h2>Beneficiaries</h2>

      {error && <div className="message">{error}</div>}

      <form className="form-grid" onSubmit={handleSubmit}>
        <div className="field">
          <label>Name</label>
          <input value={form.name} onChange={handleChange('name')} required />
        </div>
        <div className="field">
          <label>Account Number</label>
          <input value={form.accountNumber} onChange={handleChange('accountNumber')} required />
        </div>
        <div className="field">
          <label>Bank Name</label>
          <input value={form.bankName} onChange={handleChange('bankName')} required />
        </div>
        <div className="field">
          <label>IFSC Code</label>
          <input value={form.ifscCode} onChange={handleChange('ifscCode')} placeholder="ABCD0123456" required />
        </div>
        <div className="field">
          <label>Customer ID</label>
          <input type="number" value={form.customerId} onChange={handleChange('customerId')} required />
        </div>
        <button type="submit" className="primary" disabled={loading}>
          {loading ? 'Adding...' : 'Add Beneficiary'}
        </button>
      </form>

      {beneficiaries.length === 0 ? (
        <p className="empty">No beneficiaries yet.</p>
      ) : (
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>Name</th>
              <th>Account Number</th>
              <th>Bank</th>
              <th>IFSC</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {beneficiaries.map((b) => (
              <tr key={b.id}>
                <td>{b.id}</td>
                <td>{b.name}</td>
                <td>{b.accountNumber}</td>
                <td>{b.bankName}</td>
                <td>{b.ifscCode}</td>
                <td>
                  <button onClick={() => handleDelete(b.id)}>Delete</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
