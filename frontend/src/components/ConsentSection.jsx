import { useEffect, useState } from 'react';
import { AccountApi, ConsentApi, CustomerApi } from '../api';
import { getUser } from '../auth';

const PERMISSIONS = [
  { key: 'READ_ACCOUNTS', label: 'Read accounts' },
  { key: 'READ_BALANCES', label: 'Read balances' },
  { key: 'READ_TRANSACTIONS', label: 'Read transactions' },
  { key: 'READ_BENEFICIARIES', label: 'Read beneficiaries' },
];

const STATUSES = ['AWAITING_AUTHORISATION', 'AUTHORISED', 'REJECTED', 'REVOKED', 'EXPIRED'];

// <input type="datetime-local"> value for 90 days from now (local time, no seconds)
function defaultExpiry() {
  const d = new Date(Date.now() + 90 * 24 * 60 * 60 * 1000);
  d.setMinutes(d.getMinutes() - d.getTimezoneOffset());
  return d.toISOString().slice(0, 16);
}

const emptyForm = () => ({ customerId: '', tppName: '', permissions: [], accountIds: [], expiresAt: defaultExpiry() });

const formatDate = (value) => (value ? new Date(value).toLocaleString() : '');

export default function ConsentSection() {
  const { username, roles } = getUser();
  const canCreate = roles.includes('MAKER');
  const canDecide = roles.includes('ADMIN') || roles.includes('CHECKER');

  const [consents, setConsents] = useState([]);
  const [customers, setCustomers] = useState([]);
  const [accounts, setAccounts] = useState([]);
  const [statusFilter, setStatusFilter] = useState('');
  const [form, setForm] = useState(emptyForm);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const loadConsents = async (status = statusFilter) => {
    try {
      setConsents(await ConsentApi.list(status));
    } catch (err) {
      setError(err.message);
    }
  };

  useEffect(() => {
    const loadOptions = async () => {
      try {
        const [customerList, accountList] = await Promise.all([CustomerApi.list(), AccountApi.list()]);
        setCustomers(customerList);
        setAccounts(accountList);
      } catch (err) {
        setError(err.message);
      }
    };
    loadOptions();
    loadConsents('');
  }, []);

  // A consent can only cover the chosen customer's own accounts
  const customerAccounts = accounts.filter((a) => String(a.customerId) === form.customerId);

  const handleChange = (field) => (e) => {
    const next = { ...form, [field]: e.target.value };
    if (field === 'customerId') next.accountIds = [];
    setForm(next);
  };

  const toggle = (field, value) => () => {
    const list = form[field];
    setForm({ ...form, [field]: list.includes(value) ? list.filter((v) => v !== value) : [...list, value] });
  };

  const handleFilter = (e) => {
    setStatusFilter(e.target.value);
    setError('');
    loadConsents(e.target.value);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (form.permissions.length === 0 || form.accountIds.length === 0) {
      setError('Select at least one permission and one account.');
      return;
    }
    setError('');
    setLoading(true);
    try {
      await ConsentApi.create({
        customerId: Number(form.customerId),
        tppName: form.tppName,
        permissions: form.permissions,
        accountIds: form.accountIds,
        expiresAt: form.expiresAt,
      });
      setForm(emptyForm());
      await loadConsents();
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  // action: 'approve' | 'reject' | 'revoke'
  const decide = (consent, action) => async () => {
    let reason;
    if (action !== 'approve') {
      reason = window.prompt(`Reason to ${action} consent ${consent.id} (optional):`);
      if (reason === null) return; // cancelled
    }
    setError('');
    try {
      await ConsentApi[action](consent.id, reason);
      await loadConsents();
    } catch (err) {
      setError(err.message);
    }
  };

  return (
    <div>
      <h2>Consents</h2>
      <p>
        A third-party provider (TPP) asks for access to a customer&apos;s accounts. A <b>MAKER</b> raises the
        request, then a different <b>CHECKER</b> or <b>ADMIN</b> approves or rejects it.
      </p>

      {error && <div className="message">{error}</div>}

      {canCreate && (
        <form className="form-grid" onSubmit={handleSubmit}>
          <h3>New consent request</h3>
          <div className="field">
            <label>Customer</label>
            <select value={form.customerId} onChange={handleChange('customerId')} required>
              <option value="">Select a customer</option>
              {customers.map((c) => (
                <option key={c.id} value={c.id}>{c.id} - {c.name}</option>
              ))}
            </select>
          </div>
          <div className="field">
            <label>Third-party provider</label>
            <input value={form.tppName} onChange={handleChange('tppName')} placeholder="e.g. Budget App Ltd" required />
          </div>
          <fieldset className="field">
            <legend>Accounts</legend>
            {!form.customerId && <span className="empty">Select a customer first.</span>}
            {form.customerId && customerAccounts.length === 0 && <span className="empty">This customer has no accounts.</span>}
            {customerAccounts.map((a) => (
              <label key={a.id} className="check">
                <input type="checkbox" checked={form.accountIds.includes(a.id)} onChange={toggle('accountIds', a.id)} />
                {a.accountNumber} ({a.accountType})
              </label>
            ))}
          </fieldset>
          <fieldset className="field">
            <legend>Permissions</legend>
            {PERMISSIONS.map((p) => (
              <label key={p.key} className="check">
                <input type="checkbox" checked={form.permissions.includes(p.key)} onChange={toggle('permissions', p.key)} />
                {p.label}
              </label>
            ))}
          </fieldset>
          <div className="field">
            <label>Expires at</label>
            <input type="datetime-local" value={form.expiresAt} onChange={handleChange('expiresAt')} required />
          </div>
          <button type="submit" className="primary" disabled={loading}>
            {loading ? 'Submitting...' : 'Request Consent'}
          </button>
        </form>
      )}

      <div className="section-title">
        <h3>Consent requests</h3>
        <select value={statusFilter} onChange={handleFilter} aria-label="Filter by status">
          <option value="">All statuses</option>
          {STATUSES.map((s) => (
            <option key={s} value={s}>{s}</option>
          ))}
        </select>
      </div>

      {consents.length === 0 ? (
        <p className="empty">No consents found.</p>
      ) : (
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>Customer</th>
              <th>Provider</th>
              <th>Accounts</th>
              <th>Permissions</th>
              <th>Status</th>
              <th>Expires</th>
              <th>Requested by</th>
              <th>Decided by</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {consents.map((c) => {
              const pending = c.status === 'AWAITING_AUTHORISATION';
              const ownRequest = c.createdBy === username;
              return (
                <tr key={c.id}>
                  <td>{c.id}</td>
                  <td>{c.customerName}</td>
                  <td>{c.tppName}</td>
                  <td>{c.accountNumbers.join(', ')}</td>
                  <td>{c.permissions.join(', ')}</td>
                  <td>
                    {c.status}
                    {c.rejectionReason && <div className="empty">{c.rejectionReason}</div>}
                  </td>
                  <td>{formatDate(c.expiresAt)}</td>
                  <td>{c.createdBy}</td>
                  <td>{c.decidedBy && `${c.decidedBy}, ${formatDate(c.decidedAt)}`}</td>
                  <td>
                    {canDecide && pending && (
                      <div className="row-actions">
                        <button
                          onClick={decide(c, 'approve')}
                          disabled={ownRequest}
                          title={ownRequest ? 'You raised this request, so another user must decide it' : ''}
                        >
                          Approve
                        </button>
                        <button onClick={decide(c, 'reject')} disabled={ownRequest}>Reject</button>
                      </div>
                    )}
                    {canDecide && c.status === 'AUTHORISED' && <button onClick={decide(c, 'revoke')}>Revoke</button>}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      )}
    </div>
  );
}
