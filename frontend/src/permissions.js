import { getUser } from './auth';

// Which realm roles may perform each action. Mirrors the backend rules in SecurityConfig.java:
// keep the two in sync, the backend is what actually enforces them.
const RULES = {
  manageCustomers: ['ADMIN'], // create / edit / delete
  createAccount: ['ADMIN'],
  createTransaction: ['MAKER'],
  createTransfer: ['MAKER'],
  deleteBeneficiary: ['ADMIN', 'CHECKER'],
  requestConsent: ['MAKER'],
  decideConsent: ['ADMIN', 'CHECKER'], // approve / reject / revoke
};

export function can(action) {
  const { roles } = getUser();
  return RULES[action].some((role) => roles.includes(role));
}

// "ADMIN", "ADMIN or CHECKER": for telling users which role they'd need
export function rolesFor(action) {
  return RULES[action].join(' or ');
}
