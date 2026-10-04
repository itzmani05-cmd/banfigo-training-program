import { useState } from 'react';
import { TransactionApi } from '../api';

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
      <div className="section-title">
        <h2>Transactions</h2>
      </div>

      {error && <div className="message">{error}</div>}

      <div className="field" style={{ maxWidth: 420, marginBottom: 16 }}>
        <label>Account ID</label>
        <div className="row-actions">
          <input type="number" value={accountId} onChange={(e) => setAccountId(e.target.value)} />
          <button onClick={load}>Load</button>
        </div>
      </div>

      <form className="form-grid" onSubmit={handleSubmit}>
        <div className="field">
          <label>Type</label>
          <select value={form.transactionType} onChange={handleChange('transactionType')}>
            <option value="DEPOSIT">DEPOSIT</option>
            <option value="WITHDRAWAL">WITHDRAWAL</option>
          </select>
        </div>
        <div className="field">
          <label>Amount</label>
          <input type="number" min="1" value={form.amount} onChange={handleChange('amount')} required />
        </div>
        <div className="field">
          <label>Description</label>
          <input value={form.description} onChange={handleChange('description')} />
        </div>
        <button type="submit" className="primary" disabled={loading}>
          {loading ? 'Submitting...' : 'Submit Transaction'}
        </button>
      </form>

      {loaded && (
        transactions.length === 0 ? (
          <p className="empty">No transactions for this account.</p>
        ) : (
          <table>
            <thead>
              <tr>
                <th>ID</th>
                <th>Type</th>
                <th>Amount</th>
                <th>Description</th>
                <th>Date</th>
              </tr>
            </thead>
            <tbody>
              {transactions.map((t) => (
                <tr key={t.id}>
                  <td>{t.id}</td>
                  <td>{t.transactionType}</td>
                  <td>{t.amount}</td>
                  <td>{t.description}</td>
                  <td>{t.transactionDate}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )
      )}
    </div>
  );
}
