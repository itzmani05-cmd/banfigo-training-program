import { useState } from 'react';
import { TransferApi } from '../api';

const EMPTY_FORM = { fromAccountId: '', toAccountId: '', amount: '', description: '' };

export default function TransferSection() {
  const [form, setForm] = useState(EMPTY_FORM);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState(null);

  const handleChange = (field) => (e) => setForm({ ...form, [field]: e.target.value });

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (form.fromAccountId === form.toAccountId) {
      setError('Source and destination accounts must be different.');
      return;
    }
    setError('');
    setResult(null);
    setLoading(true);
    try {
      const transfer = await TransferApi.create({
        ...form,
        fromAccountId: Number(form.fromAccountId),
        toAccountId: Number(form.toAccountId),
        amount: Number(form.amount),
      });
      setResult(transfer);
      setForm(EMPTY_FORM);
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

      <form className="form-grid" onSubmit={handleSubmit}>
        <div className="field">
          <label>From Account ID</label>
          <input type="number" value={form.fromAccountId} onChange={handleChange('fromAccountId')} required />
        </div>
        <div className="field">
          <label>To Account ID</label>
          <input type="number" value={form.toAccountId} onChange={handleChange('toAccountId')} required />
        </div>
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
              <th>To Account</th>
              <td>{result.toAccountId}</td>
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
