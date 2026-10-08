import { getUser } from './auth';

// Which realm roles may perform each action. Mirrors the backend rules in SecurityConfig.java:
// keep the two in sync, the backend is what actually enforces them.
const RULES = {
  manageCustomers: ['ADMIN'], // create / edit / delete
  createAccount: ['ADMIN'],
  createTransaction: ['MAKER'],
  createTransfer: ['MAKER', 'CUSTOMER'], // a customer only from their own accounts
  deleteBeneficiary: ['ADMIN', 'CHECKER', 'CUSTOMER'], // a customer only their own
  requestConsent: ['MAKER'],
  decideConsent: ['ADMIN', 'CHECKER'], // approve / reject / revoke
};

const STAFF_ROLES = ['ADMIN', 'MAKER', 'CHECKER'];

export function can(action) {
  const { roles } = getUser();
  return RULES[action].some((role) => roles.includes(role));
}

// "ADMIN", "ADMIN or CHECKER": for telling users which role they'd need
export function rolesFor(action) {
  return RULES[action].filter((role) => role !== 'CUSTOMER').join(' or ');
}

// A self-registered customer: the backend only returns their own records.
// A staff role wins, same as CurrentUser.java.
export function isCustomer() {
  const { roles } = getUser();
  return roles.includes('CUSTOMER') && !roles.some((role) => STAFF_ROLES.includes(role));
}
