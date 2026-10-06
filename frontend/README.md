# Banking Frontend

React + Vite frontend for the Mini Banking API. It lets you manage customers, bank accounts, transactions, and beneficiaries through the Spring Boot backend.

## Tech Stack

- React 19
- Vite
- Plain `fetch` for API calls (no UI framework)
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
| `VITE_KEYCLOAK_REALM` | `Banfigo` | Browser (`src/auth.js`) | Keycloak realm |
| `VITE_KEYCLOAK_CLIENT_ID` | `banfigo-frontend` | Browser (`src/auth.js`) | Keycloak client used for login |

To override values on your machine without changing the shared file, create `frontend/.env.local` (git-ignored):

```properties
BACKEND_URL=http://localhost:9090
```

Only variables prefixed with `VITE_` are included in the browser bundle, so `BACKEND_URL` stays on the dev server. Restart `npm run dev` after changing any `.env` file.

## Authentication (Keycloak)

The app redirects to Keycloak on load and sends the access token as `Authorization: Bearer <token>` with every API call (`src/auth.js`, `src/api.js`). Tokens are refreshed automatically.

Create the client once in the Keycloak admin console (realm `Banfigo`):

1. **Clients → Create client**, Client ID `banfigo-frontend`
2. **Client authentication**: Off (public client), **Standard flow**: On
3. **Valid redirect URIs**: `http://localhost:5173/*`
4. **Valid post logout redirect URIs**: `http://localhost:5173/*`
5. **Web origins**: `http://localhost:5173`

Users need realm roles for anything beyond viewing: `ADMIN` (create accounts, create/edit/delete customers, delete beneficiaries), `MAKER` (create transactions), `CHECKER` (delete beneficiaries).

## Pages

| Tab | Features |
|---|---|
| Customers | List, create, edit, and delete customers |
| Accounts | List and create accounts; **View** opens account details (customer info + transaction history) |
| Transactions | Load an account's transaction history; create deposits and withdrawals |
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
