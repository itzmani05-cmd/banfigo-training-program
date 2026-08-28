const BASE_URL = '/api';

async function request(path, options = {}) {
  const response = await fetch(`${BASE_URL}${path}`, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  });

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
  create: (data) => request('/accounts', { method: 'POST', body: JSON.stringify(data) }),
};

export const BeneficiaryApi = {
  list: () => request('/beneficiaries'),
  create: (data) => request('/beneficiaries', { method: 'POST', body: JSON.stringify(data) }),
  remove: (id) => request(`/beneficiaries/${id}`, { method: 'DELETE' }),
};

export const TransactionApi = {
  listByAccount: (accountId) => request(`/accounts/${accountId}/transactions`),
  create: (accountId, data) => request(`/accounts/${accountId}/transactions`, { method: 'POST', body: JSON.stringify(data) }),
};
