import { useEffect, useState } from 'react';
import { AccountApi } from '../api';
import AccountDetails from './AccountDetails';

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
      <h2>Bank Accounts</h2>

      {error && <div className="message">{error}</div>}

      <form className="form-grid" onSubmit={handleSubmit}>
        <div className="field">
          <label>Account Number (10 digits)</label>
          <input value={form.accountNumber} onChange={handleChange('accountNumber')} required />
        </div>
        <div className="field">
          <label>Account Type</label>
          <input value={form.accountType} onChange={handleChange('accountType')} placeholder="SAVINGS / CURRENT" required />
        </div>
        <div className="field">
          <label>Customer ID</label>
          <input type="number" value={form.customerId} onChange={handleChange('customerId')} required />
        </div>
        <button type="submit" className="primary" disabled={loading}>
          {loading ? 'Creating...' : 'Create Account'}
        </button>
      </form>

      {accounts.length === 0 ? (
        <p className="empty">No accounts yet.</p>
      ) : (
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>Account Number</th>
              <th>Type</th>
              <th>Balance</th>
              <th>Customer ID</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {accounts.map((a) => (
              <tr key={a.id}>
                <td>{a.id}</td>
                <td>{a.accountNumber}</td>
                <td>{a.accountType}</td>
                <td>{a.balance}</td>
                <td>{a.customer?.id}</td>
                <td>
                  <button onClick={() => setSelectedId(a.id)}>View</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
