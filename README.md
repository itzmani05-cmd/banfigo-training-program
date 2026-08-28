# Banking API

A simple banking backend (Spring Boot) with a React frontend, built for the Week 1 backend assessment. Supports managing customers, bank accounts, transactions (deposit/withdrawal), and beneficiaries.

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

Runs on `http://localhost:5173` by default. API calls to `/api/*` are proxied to the backend (see `frontend/vite.config.js` — update the `target` there if you change the backend port).

## API Endpoints

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

### Beneficiaries — `/api/beneficiaries`
| Method | Path | Description |
|---|---|---|
| POST | `/` | Add a beneficiary |
| GET | `/` | List all beneficiaries |
| DELETE | `/{id}` | Remove a beneficiary |

## Notes

- Validation errors and "not found" errors return a consistent JSON error shape (see `GlobalExceptionHandler`).
- New accounts start with a balance of `0`; deposits/withdrawals go through the transactions endpoint.
- Withdrawals are rejected with an error if they would overdraw the account.
