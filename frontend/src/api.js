import { getToken } from './auth';

const BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api';

async function request(path, options = {}) {
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

  if (response.status === 204) return null;
  return response.json();
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

export const TransactionApi = {
  // params: { page, size, from, to, type } — empty values are skipped. Returns a page object.
  listByAccount: (accountId, params = {}) => {
    const query = new URLSearchParams(
      Object.entries(params).filter(([, v]) => v !== '' && v !== null && v !== undefined)
    ).toString();
    return request(`/accounts/${accountId}/transactions${query ? `?${query}` : ''}`);
  },
  create: (accountId, data) => request(`/accounts/${accountId}/transactions`, { method: 'POST', body: JSON.stringify(data) }),
};
