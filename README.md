# Banking API

A simple banking backend (Spring Boot) with a React frontend, built for the Week 1 backend assessment. Supports managing customers, bank accounts, transactions (deposit/withdrawal), transfers between accounts, and beneficiaries.

## Tech Stack

**Backend**
- Java 21
- Spring Boot 4.1.0 (Web, Data JPA, Validation)
- PostgreSQL
- Maven

**Frontend**
- React 19 (Vite)
- Plain `fetch` for API calls, no UI framework

## Project Structure

```
src/main/java/com/example/week1_backend_assesment/
├── controller/    REST endpoints (HTTP layer only)
├── service/       business logic
├── repository/    Spring Data JPA interfaces (DB access)
├── entity/        JPA entities (map to DB tables)
├── dto/           request/response shapes for the API
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

## API Endpoints

### Health & Info
| Method | Path | Description |
|---|---|---|
| GET | `/health` | Application and database status (`503` if the database is unreachable) |
| GET | `/api/info` | Application name, description, version, and Java version |

Both endpoints are public (no token required).

### Customers — `/api/customers`
| Method | Path | Description |
|---|---|---|
| POST | `/` | Create a customer |
| GET | `/` | List all customers |
| GET | `/{id}` | Get a customer by id |
| PUT | `/{id}` | Update a customer |
| DELETE | `/{id}` | Delete a customer |

### Bank Accounts — `/api/accounts`
| Method | Path | Description |
|---|---|---|
| POST | `/` | Open a new account for a customer |
| GET | `/` | List all accounts |
| GET | `/{accountId}` | Get an account by id |

### Transactions — `/api/accounts/{accountId}/transactions`
| Method | Path | Description |
|---|---|---|
| POST | `/` | Create a deposit or withdrawal |
| GET | `/` | List transactions for an account |

### Transfers — `/api/transfers`
| Method | Path | Description |
|---|---|---|
| POST | `/` | Move money to another account or a saved beneficiary (requires `MAKER` role) |

Request body: `{ "fromAccountId": 1, "toAccountId": 2, "amount": 250.00, "description": "Rent" }`, or with `"beneficiaryId": 5` instead of `toAccountId`.

Paying a beneficiary: the source account must belong to the customer who saved the beneficiary. If the beneficiary's account number is an account in this bank, it is credited like a normal transfer; otherwise only the debit is recorded (an outgoing payment to another bank).

A transfer is all-or-nothing: it writes a `WITHDRAWAL` on the source account and a `DEPOSIT` on the destination, both with the same `reference`, in one database transaction. Both account rows are locked (lowest id first) so concurrent transfers can't overdraw an account or deadlock.

### Statements — `/api/accounts/{accountId}/statement`
| Method | Path | Description |
|---|---|---|
| GET | `/?from=2026-10-01&to=2026-10-31&format=pdf` | Download a statement as `pdf` or `csv` (default) |

Includes the opening balance, every transaction with a running balance, total debits/credits, and the closing balance. The period can be at most one year.

### Dashboard — `/api/dashboard`
| Method | Path | Description |
|---|---|---|
| GET | `/` | Total balance, account and customer counts, 10 most recent transactions, and money in/out for the last 6 months |

### Beneficiaries — `/api/beneficiaries`
| Method | Path | Description |
|---|---|---|
| POST | `/` | Add a beneficiary |
| GET | `/` | List all beneficiaries |
| DELETE | `/{id}` | Remove a beneficiary |

### Consents — `/api/consents`
Open Banking style consents: a customer lets a third-party provider (TPP) access some of their accounts. Requests follow a maker-checker flow.

| Method | Path | Role | Description |
|---|---|---|---|
| POST | `/` | `MAKER` | Create a consent request (status `AWAITING_AUTHORISATION`) |
| GET | `/?status=AUTHORISED` | any logged-in user | List consents, newest first (`status` is optional) |
| GET | `/{id}` | any logged-in user | Get a consent by id |
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

## Notes

- Validation errors and "not found" errors return a consistent JSON error shape (see `GlobalExceptionHandler`).
- New accounts start with a balance of `0`; deposits/withdrawals go through the transactions endpoint.
- Withdrawals are rejected with an error if they would overdraw the account.
