import Keycloak from 'keycloak-js';

const keycloak = new Keycloak({
  url: import.meta.env.VITE_KEYCLOAK_URL || 'http://localhost:8086',
  realm: import.meta.env.VITE_KEYCLOAK_REALM || 'BanfigoNew',
  clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID || 'BanfigoFrontend',
});

// Redirects to the Keycloak login page if the user isn't signed in.
// Called once from main.jsx, before React renders (keycloak-js can't be initialised twice).
export function initAuth() {
  return keycloak.init({
    onLoad: 'login-required',
    pkceMethod: 'S256',
    checkLoginIframe: false,
  });
}

// Returns a valid access token, refreshing it first if it expires within 30 seconds.
export async function getToken() {
  try {
    await keycloak.updateToken(30);
  } catch {
    // Refresh token expired too: send the user back to the login page
    await keycloak.login();
  }
  return keycloak.token;
}

export function getUser() {
  const parsed = keycloak.tokenParsed || {};
  return {
    username: parsed.preferred_username,
    roles: parsed.realm_access?.roles || [],
  };
}

export function logout() {
  return keycloak.logout({ redirectUri: window.location.origin });
}
