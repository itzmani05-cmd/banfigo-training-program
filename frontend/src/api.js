import { getToken } from './auth';

const BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api';

// Sends an authenticated request and throws a readable Error on failure. Returns the raw Response.
async function send(path, options = {}) {
  const token = await getToken();
  const response = await fetch(`${BASE_URL}${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
    },
  });

  if (response.status === 401) throw new Error('Your session has expired. Please log in again.');
  if (response.status === 403) throw new Error('You do not have permission to perform this action.');

  if (!response.ok) {
    const body = await response.json().catch(() => null);
    const message = body?.message || body?.details?.join(', ') || `Request failed (${response.status})`;
    throw new Error(message);
  }

  return response;
}

async function request(path, options = {}) {
  const response = await send(path, options);
  if (response.status === 204) return null;
  return response.json();
}

// Fetches a file and makes the browser save it (a plain link can't carry the Bearer token)
async function download(path) {
  const response = await send(path);
  const disposition = response.headers.get('Content-Disposition') || '';
  const fileName = disposition.match(/filename="?([^";]+)"?/)?.[1] || 'download';

  const url = URL.createObjectURL(await response.blob());
  const link = document.createElement('a');
  link.href = url;
  link.download = fileName;
  link.click();
  // Revoking in the same tick can cancel the download in some browsers
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}

function toQuery(params) {
  const query = new URLSearchParams(
    Object.entries(params).filter(([, v]) => v !== '' && v !== null && v !== undefined)
  ).toString();
  return query ? `?${query}` : '';
}

export const CustomerApi = {
  list: () => request('/customers'),
  create: (data) => request('/customers', { method: 'POST', body: JSON.stringify(data) }),
  update: (id, data) => request(`/customers/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
  remove: (id) => request(`/customers/${id}`, { method: 'DELETE' }),
};

export const AccountApi = {
  list: () => request('/accounts'),
  get: (id) => request(`/accounts/${id}`),
  create: (data) => request('/accounts', { method: 'POST', body: JSON.stringify(data) }),
};

export const BeneficiaryApi = {
  list: () => request('/beneficiaries'),
  create: (data) => request('/beneficiaries', { method: 'POST', body: JSON.stringify(data) }),
  remove: (id) => request(`/beneficiaries/${id}`, { method: 'DELETE' }),
};

// params: { from, to, format: 'csv' | 'pdf' }
export const StatementApi = {
  download: (accountId, params) => download(`/accounts/${accountId}/statement${toQuery(params)}`),
};

export const DashboardApi = {
  get: () => request('/dashboard'),
};

export const ConsentApi = {
  // status: optional ConsentStatus filter, e.g. 'AWAITING_AUTHORISATION'
  list: (status) => request(`/consents${toQuery({ status })}`),
  create: (data) => request('/consents', { method: 'POST', body: JSON.stringify(data) }),
  approve: (id) => request(`/consents/${id}/approve`, { method: 'POST' }),
  reject: (id, reason) => request(`/consents/${id}/reject`, { method: 'POST', body: JSON.stringify({ reason }) }),
  revoke: (id, reason) => request(`/consents/${id}/revoke`, { method: 'POST', body: JSON.stringify({ reason }) }),
};

export const TransferApi = {
  create: (data) => request('/transfers', { method: 'POST', body: JSON.stringify(data) }),
};

export const TransactionApi = {
  // params: { page, size, from, to, type } — empty values are skipped. Returns a page object.
  listByAccount: (accountId, params = {}) => request(`/accounts/${accountId}/transactions${toQuery(params)}`),
  create: (accountId, data) => request(`/accounts/${accountId}/transactions`, { method: 'POST', body: JSON.stringify(data) }),
};
