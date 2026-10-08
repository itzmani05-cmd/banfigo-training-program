import { useEffect, useState } from 'react';
import { AccountApi, ConsentApi, CustomerApi } from '../api';
import { getUser } from '../auth';
import { can, isCustomer, rolesFor } from '../permissions';
import { Alert, Badge, Button, Card, Empty, Field, FormActions, FormGrid, PageHeader, Table, Td, ViewOnly, inputClass } from './ui';

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

const STATUS_TONES = { AWAITING_AUTHORISATION: 'outline', AUTHORISED: 'solid' };
const statusLabel = (status) => (status === 'AWAITING_AUTHORISATION' ? 'AWAITING' : status);

// Checkbox group; a fieldset because a <label> can't wrap several inputs
function CheckGroup({ legend, children, className = '' }) {
  return (
    <fieldset className={`rounded-md border border-line px-4 pt-2 pb-3 ${className}`}>
      <legend className="px-1 text-label font-bold">{legend}</legend>
      <div className="grid gap-2 sm:grid-cols-2">{children}</div>
    </fieldset>
  );
}

function Check({ checked, onChange, children }) {
  return (
    <label className="flex cursor-pointer items-center gap-2">
      <input type="checkbox" checked={checked} onChange={onChange} className="size-4 accent-ink" />
      {children}
    </label>
  );
}

export default function ConsentSection() {
  const { username } = getUser();
  const canCreate = can('requestConsent');
  const canDecide = can('decideConsent');

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
      <PageHeader
        title="Consents"
        description={
          isCustomer()
            ? 'Third-party providers (TPPs) that have asked for access to your accounts, and whether the bank approved it.'
            : "A third-party provider (TPP) asks for access to a customer's accounts. A MAKER raises the request, then a different CHECKER or ADMIN approves or rejects it."
        }
      />

      <Alert>{error}</Alert>

      {!canCreate && !canDecide && !isCustomer() && (
        <ViewOnly>
          You can view consents. Requesting one requires the {rolesFor('requestConsent')} role; approving, rejecting or
          revoking requires {rolesFor('decideConsent')}.
        </ViewOnly>
      )}

      {canCreate && (
        <Card title="New consent request">
          <FormGrid onSubmit={handleSubmit}>
            <Field label="Customer">
              <select className={inputClass} value={form.customerId} onChange={handleChange('customerId')} required>
                <option value="">Select a customer</option>
                {customers.map((c) => (
                  <option key={c.id} value={c.id}>{c.id} - {c.name}</option>
                ))}
              </select>
            </Field>
            <Field label="Third-party provider">
              <input className={inputClass} value={form.tppName} onChange={handleChange('tppName')} placeholder="e.g. Budget App Ltd" required />
            </Field>
            <CheckGroup legend="Accounts">
              {!form.customerId && <span className="text-muted italic">Select a customer first.</span>}
              {form.customerId && customerAccounts.length === 0 && <span className="text-muted italic">This customer has no accounts.</span>}
              {customerAccounts.map((a) => (
                <Check key={a.id} checked={form.accountIds.includes(a.id)} onChange={toggle('accountIds', a.id)}>
                  <span className="tabular-nums">{a.accountNumber}</span>
                  <span className="text-muted">({a.accountType})</span>
                </Check>
              ))}
            </CheckGroup>
            <CheckGroup legend="Permissions">
              {PERMISSIONS.map((p) => (
                <Check key={p.key} checked={form.permissions.includes(p.key)} onChange={toggle('permissions', p.key)}>
                  {p.label}
                </Check>
              ))}
            </CheckGroup>
            <Field label="Expires at">
              <input className={inputClass} type="datetime-local" value={form.expiresAt} onChange={handleChange('expiresAt')} required />
            </Field>
            <FormActions>
              <Button type="submit" variant="primary" disabled={loading}>
                {loading ? 'Submitting...' : 'Request Consent'}
              </Button>
            </FormActions>
          </FormGrid>
        </Card>
      )}

      <Card
        title={`Consent requests (${consents.length})`}
        actions={
          <select className={`${inputClass} w-auto`} value={statusFilter} onChange={handleFilter} aria-label="Filter by status">
            <option value="">All statuses</option>
            {STATUSES.map((s) => (
              <option key={s} value={s}>{s}</option>
            ))}
          </select>
        }
      >
        {consents.length === 0 ? (
          <Empty>No consents found.</Empty>
        ) : (
          <Table columns={['ID', 'Customer / Provider', 'Access', 'Status', 'Expires', 'Requested / Decided', ...(canDecide ? [''] : [])]}>
            {consents.map((c) => {
              const pending = c.status === 'AWAITING_AUTHORISATION';
              const ownRequest = c.createdBy === username;
              return (
                <tr key={c.id} className="hover:bg-wash/60">
                  <Td className="text-muted">{c.id}</Td>
                  <Td>
                    <div className="font-bold">{c.customerName}</div>
                    <div className="text-muted">{c.tppName}</div>
                  </Td>
                  <Td>
                    <div className="tabular-nums">{c.accountNumbers.join(', ')}</div>
                    <div className="mt-1 flex flex-wrap gap-1">
                      {c.permissions.map((p) => <Badge key={p}>{p.replace('READ_', '')}</Badge>)}
                    </div>
                  </Td>
                  <Td>
                    <Badge tone={STATUS_TONES[c.status]}>{statusLabel(c.status)}</Badge>
                    {c.rejectionReason && <div className="mt-1 text-label text-muted italic">{c.rejectionReason}</div>}
                  </Td>
                  <Td className="whitespace-nowrap text-muted">{formatDate(c.expiresAt)}</Td>
                  <Td className="text-label">
                    <div>{c.createdBy}</div>
                    {c.decidedBy && <div className="text-muted">{c.decidedBy}, {formatDate(c.decidedAt)}</div>}
                  </Td>
                  {canDecide && (
                    <Td className="text-right">
                      {pending && (
                        <div className="flex justify-end gap-2">
                          <Button
                            size="sm"
                            variant="primary"
                            onClick={decide(c, 'approve')}
                            disabled={ownRequest}
                            title={ownRequest ? 'You raised this request, so another user must decide it' : ''}
                          >
                            Approve
                          </Button>
                          <Button size="sm" onClick={decide(c, 'reject')} disabled={ownRequest}>Reject</Button>
                        </div>
                      )}
                      {c.status === 'AUTHORISED' && (
                        <Button size="sm" variant="ghost" onClick={decide(c, 'revoke')}>Revoke</Button>
                      )}
                    </Td>
                  )}
                </tr>
              );
            })}
          </Table>
        )}
      </Card>
    </div>
  );
}
