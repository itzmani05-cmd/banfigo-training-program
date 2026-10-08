# Banking Frontend

React + Vite frontend for the Mini Banking API. It lets you manage customers, bank accounts, transactions, and beneficiaries through the Spring Boot backend.

## Tech Stack

- React 19
- Vite
- Tailwind CSS v4 for styling (design tokens in `src/index.css`, shared components in `src/components/ui.jsx`)
- Plain `fetch` for API calls
- Oxlint for linting

## Prerequisites

- Node.js 18+
- Backend running (see the root `README.md`), by default on `http://localhost:8082`

## Setup

```bash
cd frontend
npm install
npm run dev
```

The app runs on `http://localhost:5173`.

## Environment Configuration

Settings live in `frontend/.env`:

| Variable | Default | Used by | Description |
|---|---|---|---|
| `VITE_API_BASE_URL` | `/api` | Browser (`src/api.js`) | Base URL for all API calls |
| `BACKEND_URL` | `http://localhost:8082` | Vite dev server (`vite.config.js`) | Where `/api` requests are proxied in development |
| `VITE_KEYCLOAK_URL` | `http://localhost:8086` | Browser (`src/auth.js`) | Keycloak server URL |
| `VITE_KEYCLOAK_REALM` | `BanfigoNew` | Browser (`src/auth.js`) | Keycloak realm |
| `VITE_KEYCLOAK_CLIENT_ID` | `BanfigoFrontend` | Browser (`src/auth.js`) | Keycloak client used for login |

To override values on your machine without changing the shared file, create `frontend/.env.local` (git-ignored):

```properties
BACKEND_URL=http://localhost:9090
```

Only variables prefixed with `VITE_` are included in the browser bundle, so `BACKEND_URL` stays on the dev server. Restart `npm run dev` after changing any `.env` file.

## Authentication (Keycloak)

The app redirects to Keycloak on load and sends the access token as `Authorization: Bearer <token>` with every API call (`src/auth.js`, `src/api.js`). Tokens are refreshed automatically.

Create the client once in the Keycloak admin console (realm `BanfigoNew`):

1. **Clients → Create client**, Client ID `BanfigoFrontend`
2. **Client authentication**: Off (public client), **Standard flow**: On
3. **Valid redirect URIs**: `http://localhost:5173/*`
4. **Valid post logout redirect URIs**: `http://localhost:5173/*`
5. **Web origins**: `http://localhost:5173`

Every user needs one of these realm roles. A user without one gets `403` from the API.

| Role | Can do |
|---|---|
| `ADMIN` | Create accounts; create, edit and delete customers; delete beneficiaries; approve, reject and revoke consents |
| `MAKER` | Create transactions and transfers; request consents |
| `CHECKER` | Delete beneficiaries; approve, reject and revoke consents |
| `CUSTOMER` | Given automatically to anyone who signs up with **Register** on the login page. Sees only their own accounts, transactions, beneficiaries and consents, and makes transfers from their own accounts |

All staff roles can view everything. A CUSTOMER doesn't see the **Customers** and **Transactions** tabs; their history is
under **Accounts → View**. The UI hides actions a role can't perform, but the backend is what enforces the rules
(`src/permissions.js` mirrors `SecurityConfig.java`).

To try the consent flow, log in as a `MAKER` user to raise a request on the **Consents** tab, then log in as a different `CHECKER` or `ADMIN` user to approve or reject it. The same user can't do both.

## Pages

| Tab | Features |
|---|---|
| Dashboard | Total balance, account/customer counts, money in vs out chart (last 6 months), recent transactions |
| Customers | List, create, edit, and delete customers |
| Accounts | List and create accounts; **View** opens account details (customer info, statement download as PDF/CSV, transaction history) |
| Transactions | Load an account's transaction history; create deposits and withdrawals |
| Transfers | Move money to another account or pay a saved beneficiary; shows the transfer reference and the source balance afterwards |
| Beneficiaries | List, add, and delete beneficiaries |

## Error Handling

All API calls go through `request()` in `src/api.js`. When the backend returns an error, the message from the backend's JSON error body (`message` or validation `details`) is shown at the top of the page. If there is no body, the HTTP status code is shown.

## Project Structure

```
src/
├── components/
│   ├── CustomerSection.jsx
│   ├── AccountSection.jsx
│   ├── AccountDetails.jsx
│   ├── TransactionSection.jsx
│   ├── TransferSection.jsx
│   ├── Dashboard.jsx
│   ├── MonthlyFlowChart.jsx
│   └── BeneficiarySection.jsx
├── api.js        fetch wrappers for the backend API
├── App.jsx       tab navigation
└── main.jsx      entry point
```

## Scripts

| Command | Description |
|---|---|
| `npm run dev` | Start the dev server |
| `npm run build` | Build for production into `dist/` |
| `npm run preview` | Preview the production build |
| `npm run lint` | Run Oxlint |
