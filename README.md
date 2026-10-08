# Banking API

Mini Banking / Open Banking Consent Management System: a Spring Boot backend and React frontend, secured with Keycloak and run behind an Nginx gateway with Docker Compose. Supports customers, bank accounts, transactions (deposit/withdrawal), transfers, beneficiaries, statements and Open Banking consents with maker-checker approval.

## Tech Stack

**Backend**
- Java 21
- Spring Boot 4.1.0 (Web, Data JPA, Validation)
- PostgreSQL
- Maven

**Frontend**
- React 19 (Vite)
- Tailwind CSS v4
- Plain `fetch` for API calls

## Project Structure

The backend is organised **by feature**: each business area is its own package, and inside it the code is split by layer.

```
src/main/java/com/example/banfigo/
├── BanfigoApplication.java
├── customer/
│   ├── controller/    REST endpoints (HTTP layer only)
│   ├── service/       business logic
│   ├── repository/    Spring Data JPA interfaces (DB access)
│   ├── entity/        JPA entities (map to DB tables)
│   └── dto/           request/response shapes for the API
├── account/           bank accounts and statements (PDF/CSV)
├── transaction/       deposits and withdrawals
├── transfer/          transfers between accounts and beneficiary payments
├── beneficiary/       saved payees
├── consent/           Open Banking consents (maker-checker approval)
├── dashboard/         totals, recent activity, monthly money in/out
└── common/
    ├── config/        security (Keycloak JWT, role rules)
    ├── controller/    /health and /api/info
    ├── dto/           shared response shapes (pagination)
    └── exception/     custom exceptions + global error handling

frontend/
├── src/
│   ├── components/   one component per resource (Customers, Accounts, Transactions, Beneficiaries)
│   ├── api.js        fetch wrappers for the backend API
│   └── App.jsx        tab navigation
└── vite.config.js     dev server + API proxy config
```

## Prerequisites

- JDK 21
- Node.js 18+
- PostgreSQL running locally (or a hosted instance)

## Setup

### 1. Database

Create a database and update `src/main/resources/application.properties` with your connection details:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/student_db
spring.datasource.username=postgres
spring.datasource.password=postgres
```

Tables are created/updated automatically on startup (`spring.jpa.hibernate.ddl-auto=update`).

### 2. Backend

```bash
./mvnw spring-boot:run
```

Runs on `http://localhost:8082` (see `server.port` in `application.properties`).

### 3. Frontend

```bash
cd frontend
npm install
npm run dev
```

Runs on `http://localhost:5173` by default. API calls to `/api/*` are proxied to the backend. The backend address is set by `BACKEND_URL` in `frontend/.env` (see `frontend/README.md`).

### 4. Run Backend with Docker (optional)

```bash
docker build -t banking-api .
docker run -p 8082:8082 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/student_db \
  -e SPRING_DATASOURCE_USERNAME=postgres \
  -e SPRING_DATASOURCE_PASSWORD=postgres \
  banking-api
```

Inside a container `localhost` refers to the container itself, so the datasource URL points to `host.docker.internal` to reach PostgreSQL running on your machine.

## Run the Full Stack with Docker Compose

One command starts PostgreSQL, Keycloak, the backend, the frontend and an Nginx gateway. Everything is reached through **http://localhost:8080**.

```
browser ──▶ nginx :8080 ─┬─ /         ─▶ frontend:80     React app (static build)
                         ├─ /api/     ─▶ backend:8082    Spring Boot
                         ├─ /health   ─▶ backend:8082
                         └─ /auth/    ─▶ keycloak:8080   login + admin console
backend ──▶ postgres:5432 (data), keycloak:8080 (token signing keys)
```

Only Nginx is published on your machine; the other containers talk to each other by service name on the Compose network.

### Requirements

- Docker Desktop (or Docker Engine with the Compose plugin)
- Port **8080** free on your machine (see [Troubleshooting](#troubleshooting-docker))

### Commands

```bash
docker compose up --build -d     # build images and start everything in the background
docker compose ps                # status (keycloak takes ~30-60 s to become healthy)
docker compose logs -f nginx     # gateway access log: request, status, which upstream answered
docker compose logs -f backend   # Spring Boot log
docker compose down              # stop (data is kept)
docker compose down -v           # stop AND delete the database, Keycloak and log volumes
```

Then open:

| URL | What |
|---|---|
| http://localhost:8080 | The app (redirects to the Keycloak login) |
| http://localhost:8080/auth/admin | Keycloak admin console (`admin` / `admin`) |
| http://localhost:8080/health | Backend + database health |

### Logins

The `BanfigoNew` realm is imported automatically from `keycloak/realm-export.json` on first start (client `BanfigoFrontend`, roles `ADMIN`, `MAKER`, `CHECKER`, `CUSTOMER`):

| Username | Password | Role |
|---|---|---|
| `admin1` | `admin123` | ADMIN |
| `maker1` | `maker123` | MAKER |
| `checker1` | `checker123` | CHECKER |
| `customer1` | `customer123` | CUSTOMER |

These are demo credentials for local use only.

### Customer sign-up

Anyone can create their own login with the **Register** link on the Keycloak login page. New users get the `CUSTOMER` role automatically (it's in the realm's default roles).

- **First login:** the backend creates a `Customer` record for them, using the name and email from the token and linking it by the Keycloak user id (`customers.keycloak_user_id`). Phone and address start empty, and staff can fill them in later.
- **What they see:** customers only see their own accounts, transactions, statements, beneficiaries and consents, plus a dashboard of their own figures. Someone else's record answers `404`, the same as a missing one. `GET /api/me` returns their own customer record.
- **What they can do:** add and delete their own beneficiaries, and make transfers from their own accounts, either between their own accounts or to one of their saved beneficiaries. Deposits, withdrawals, opening accounts and consent decisions stay with staff.
- **Opening an account:** an ADMIN opens it for the customer as usual, using the customer ID shown in the Customers tab.
- **Staff roles win:** a user who has a staff role (`ADMIN`, `MAKER`, `CHECKER`) is treated as staff even if they also have `CUSTOMER`.
- **Email verification** is off, because the local stack has no mail server.

Keycloak only imports the realm file the first time it starts. If your `keycloak_data` volume already exists, either recreate it (`docker compose down`, `docker volume rm banfigo_keycloak_data`, `docker compose up -d`) or make the same changes in the admin console:
- Realm settings → Login → User registration: on.
- Create the realm role `CUSTOMER`.
- Realm settings → User registration → Default roles: add `CUSTOMER`.

### How login works behind the gateway

- Keycloak runs with `KC_HTTP_RELATIVE_PATH=/auth` and `KC_HOSTNAME=http://localhost:8080/auth`, so the login pages and the tokens it issues use the public gateway URL. Tokens carry `iss = http://localhost:8080/auth/realms/BanfigoNew`.
- The frontend image is built with `VITE_KEYCLOAK_URL=/auth` (same origin as the app, through Nginx) and `VITE_API_BASE_URL=/api`.
- The backend checks the token's issuer against the public URL (`SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI`) but downloads Keycloak's signing keys over the Docker network (`..._JWK_SET_URI=http://keycloak:8080/auth/realms/BanfigoNew/protocol/openid-connect/certs`). The role rules in `SecurityConfig` are unchanged.
- These are environment variables in `docker-compose.yml`; `application.properties` and `frontend/.env` still describe the local (non-Docker) setup, which keeps working as before.

### Rate limiting

Nginx limits requests per client IP (`limit_req` in `nginx/nginx.conf`):

| Zone | Applies to | Limit |
|---|---|---|
| `api` | every `/api/` request | 20/s, burst 40 |
| `api_write` | `POST`/`PUT`/`PATCH`/`DELETE` on `/api/` | 5/s, burst 10 |
| `login` | `POST` to Keycloak's login form and token endpoint | 10/min, burst 10 |

Over the limit, the gateway answers `429 Too Many Requests` with a `Retry-After` header and the same JSON error body the API uses, so the frontend shows the message like any other error. Rejections are logged as warnings in `docker compose logs nginx`. Keycloak's own brute-force lockout (`bruteForceProtected` in the realm) still applies on top. The local (non-Docker) setup has no gateway, so no limits apply there.

### Data

| Volume | Holds |
|---|---|
| `postgres_data` | Banking database (`student_db`). Starts empty; tables are created on first backend start. |
| `keycloak_data` | Keycloak's own database (realm, users, sessions). The realm file is imported only when the realm doesn't exist yet, so changes made in the admin console survive restarts. |
| `nginx_logs` | `access.log` / `error.log` (also printed to `docker compose logs nginx`) |

The Docker database is separate from a PostgreSQL installed on your machine, so data from local development doesn't appear in Docker and vice versa.

### Troubleshooting (Docker)

- **`Bind for 0.0.0.0:8080 failed: port is already allocated`** — something else (often XAMPP/Apache `httpd`) uses port 8080. Stop it, or run on another port by creating a `.env` file next to `docker-compose.yml`:
  ```properties
  GATEWAY_PORT=8090
  PUBLIC_URL=http://localhost:8090
  ```
  and add `http://localhost:8090/*` to the `BanfigoFrontend` client's redirect URIs and web origins in Keycloak (the realm file only allows 8080 and 5173).
- **Every API call returns 401** — the token's issuer doesn't match `PUBLIC_URL`. Open the app with exactly the URL in `PUBLIC_URL` (`localhost`, not `127.0.0.1`).
- **Realm changes in `realm-export.json` don't show up** — the import skips an existing realm. Run `docker compose down -v` to start Keycloak from scratch (this also wipes the Docker database).
- **Backend keeps restarting at first start** — it waits for PostgreSQL and Keycloak to be healthy; check `docker compose ps` and `docker compose logs keycloak`.

## API Endpoints

Every endpoint except `/health` and `/api/info` needs a Keycloak access token (`Authorization: Bearer <token>`) for a user
with one of the roles `ADMIN`, `MAKER`, `CHECKER` or `CUSTOMER`.

- **No token, or an invalid one:** `401`.
- **Valid token but the wrong role:** `403`.
- **Staff roles** (`ADMIN`, `MAKER`, `CHECKER`) see every customer's data.
- **CUSTOMER** only sees their own data. Someone else's record answers `404`, the same as a missing one.
- **Different status codes per role:** where the same endpoint is answered differently for different roles, the Role
  column says so.

| Role | In short |
|---|---|
| `ADMIN` | Manages customers and opens accounts; approves, rejects or revokes consents; deletes beneficiaries |
| `MAKER` | Moves money (deposits, withdrawals, transfers) and requests consents |
| `CHECKER` | Approves, rejects or revokes consents; deletes beneficiaries |
| `CUSTOMER` | Self-registered; sees their own data, transfers from their own accounts, manages their own beneficiaries |

### Health & Info
| Method | Path | Description |
|---|---|---|
| GET | `/health` | Application and database status (`503` if the database is unreachable) |
| GET | `/api/info` | Application name, description, version, and Java version |

Both endpoints are public (no token required).

### Customers — `/api/customers`
| Method | Path | Role | Description |
|---|---|---|---|
| POST | `/` | `ADMIN` | Create a customer |
| GET | `/` | `ADMIN`, `MAKER`, `CHECKER` | List all customers |
| GET | `/{id}` | `ADMIN`, `MAKER`, `CHECKER` | Get a customer by id |
| PUT | `/{id}` | `ADMIN` | Update a customer |
| DELETE | `/{id}` | `ADMIN` | Delete a customer (`409` while they still have accounts, beneficiaries or consents) |

### My profile — `/api/me`
| Method | Path | Role | Description |
|---|---|---|---|
| GET | `/` | `CUSTOMER` | The logged-in customer's own record. The first call creates it from the token (see [Customer sign-up](#customer-sign-up)) |

### Bank Accounts — `/api/accounts`
| Method | Path | Role | Description |
|---|---|---|---|
| POST | `/` | `ADMIN` | Open a new account for a customer |
| GET | `/` | any role (CUSTOMER: own) | List accounts |
| GET | `/{accountId}` | any role (CUSTOMER: own) | Get an account by id |

### Transactions — `/api/accounts/{accountId}/transactions`
| Method | Path | Role | Description |
|---|---|---|---|
| POST | `/` | `MAKER` | Create a deposit or withdrawal |
| GET | `/?page=0&size=10&from=&to=&type=` | any role (CUSTOMER: own) | List transactions for an account, newest first (all filters optional) |

Request body: `{ "transactionType": "DEPOSIT", "amount": 500.00, "description": "Salary" }` (`DEPOSIT` or `WITHDRAWAL`).

Amount rules for deposits, withdrawals and transfers:
- More than 0, with at most 2 decimal places.
- At most 100,000 per transaction (`banfigo.limits.max-transaction-amount`).
- A withdrawal or transfer can't take the balance below 0.

### Transfers — `/api/transfers`
| Method | Path | Description |
|---|---|---|
| POST | `/` | Move money to another account or a saved beneficiary (`MAKER`, or a `CUSTOMER` from their own account) |

A CUSTOMER can transfer account-to-account only between their own accounts. To pay anyone else they use one of their
own saved beneficiaries.

Request body: `{ "fromAccountId": 1, "toAccountId": 2, "amount": 250.00, "description": "Rent" }`, or with `"beneficiaryId": 5` instead of `toAccountId`.

Paying a beneficiary: the source account must belong to the customer who saved the beneficiary. If the beneficiary's account number is an account in this bank, it is credited like a normal transfer; otherwise only the debit is recorded (an outgoing payment to another bank).

A transfer is all-or-nothing: it writes a `WITHDRAWAL` on the source account and a `DEPOSIT` on the destination, both with the same `reference`, in one database transaction. Both account rows are locked (lowest id first) so concurrent transfers can't overdraw an account or deadlock.

**Retries are safe with an `Idempotency-Key` header.** Send a unique value, such as a UUID, with each transfer and reuse it when retrying. If a request with that key already succeeded, the API returns the original result instead of moving the money again. If two requests with the same key arrive together, only one goes through. Using the key for a different transfer, or as a different user, returns `409`. A transfer that failed (for example, insufficient balance) leaves the key unused, so it can be retried. The header is optional, but the frontend always sends it.

**Who moved the money:** every transaction row stores `createdBy`, the login name of the user who made the deposit, withdrawal or transfer. It's returned in the transaction list and shown in the "By" column. Rows from before this was added show `-`.

### Statements — `/api/accounts/{accountId}/statement`
| Method | Path | Role | Description |
|---|---|---|---|
| GET | `/?from=2026-10-01&to=2026-10-31&format=pdf` | any role (CUSTOMER: own) | Download a statement as `pdf` or `csv` (default) |

Includes the opening balance, every transaction with a running balance, total debits/credits, and the closing balance. The period can be at most one year.

### Dashboard — `/api/dashboard`
| Method | Path | Role | Description |
|---|---|---|---|
| GET | `/` | any role (CUSTOMER: own figures) | Total balance, account and customer counts, 10 most recent transactions, and money in/out for the last 6 months |

### Beneficiaries — `/api/beneficiaries`
| Method | Path | Role | Description |
|---|---|---|---|
| POST | `/` | any role (CUSTOMER: for themselves) | Add a beneficiary |
| GET | `/` | any role (CUSTOMER: own) | List beneficiaries |
| DELETE | `/{id}` | `ADMIN`, `CHECKER`, or `CUSTOMER` (own) | Remove a beneficiary |

Request body: `{ "name": "Ravi Kumar", "accountNumber": "50100234567890", "bankName": "HDFC Bank", "ifscCode": "HDFC0001234", "customerId": 1 }`.
A CUSTOMER leaves out `customerId`, because their beneficiaries are always their own.

### Consents — `/api/consents`
Open Banking style consents: a customer lets a third-party provider (TPP) access some of their accounts. Requests follow a maker-checker flow.

| Method | Path | Role | Description |
|---|---|---|---|
| POST | `/` | `MAKER` | Create a consent request (status `AWAITING_AUTHORISATION`) |
| GET | `/?status=AUTHORISED` | any role (CUSTOMER: own) | List consents, newest first (`status` is optional) |
| GET | `/{id}` | any role (CUSTOMER: own) | Get a consent by id |
| POST | `/{id}/approve` | `CHECKER` or `ADMIN` | Approve a pending consent (status `AUTHORISED`) |
| POST | `/{id}/reject` | `CHECKER` or `ADMIN` | Reject a pending consent, optional body `{ "reason": "..." }` |
| POST | `/{id}/revoke` | `CHECKER` or `ADMIN` | Withdraw an authorised consent, optional body `{ "reason": "..." }` |

Request body for create:

```json
{
  "customerId": 1,
  "tppName": "Budget App Ltd",
  "permissions": ["READ_ACCOUNTS", "READ_BALANCES", "READ_TRANSACTIONS"],
  "accountIds": [1, 2],
  "expiresAt": "2027-01-31T23:59:00"
}
```

Permissions: `READ_ACCOUNTS`, `READ_BALANCES`, `READ_TRANSACTIONS`, `READ_BENEFICIARIES`.

Rules:
- Every account in `accountIds` must belong to the customer.
- The user who created a consent can't approve or reject it. Another user has to do that (four-eyes check).
- Only `AWAITING_AUTHORISATION` consents can be approved or rejected, and only `AUTHORISED` ones can be revoked. Any other transition returns `409 Conflict`.
- Pending or authorised consents whose `expiresAt` has passed are marked `EXPIRED` automatically.

```
AWAITING_AUTHORISATION ──approve──▶ AUTHORISED ──revoke──▶ REVOKED
          │                              │
          └──reject──▶ REJECTED          └── (expiry passes) ──▶ EXPIRED
```

## Testing with Postman

The `postman/` folder has a ready-made collection (45 requests, automatic Keycloak tokens for `admin1` / `maker1` / `checker1`, pass/fail tests) and environments for the local and Docker setups, including 401 vs 403 and error-response checks. Step-by-step instructions: [`postman/README.md`](postman/README.md).

## Notes

- Validation errors and "not found" errors return a consistent JSON error shape (see `GlobalExceptionHandler`).
- New accounts start with a balance of `0`; deposits/withdrawals go through the transactions endpoint.
- Withdrawals are rejected with an error if they would overdraw the account.
