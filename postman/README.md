# Testing the Banfigo API with Postman

This folder contains everything needed to test the whole API:

| File | What it is |
|---|---|
| `banfigo.postman_collection.json` | 45 requests in 12 folders, with automatic tokens and pass/fail tests |
| `banfigo-local.postman_environment.json` | Settings for running locally (backend `:8082`, Keycloak `:8086`) |
| `banfigo-docker.postman_environment.json` | Settings for Docker Compose (everything through Nginx at `:8080`) |

---

## Step 1 — Start the application

Pick **one** way of running it.

**Option A – Local (no Docker)**

1. Start PostgreSQL.
2. Start Keycloak on port **8086** (realm `BanfigoNew`).
3. Start the backend: `./mvnw spring-boot:run` (runs on `http://localhost:8082`).
4. Check: open http://localhost:8082/health — it should show `"status": "UP"`.

**Option B – Docker**

1. Make sure nothing else uses port **8080** (stop XAMPP/Apache if it's running).
2. `docker compose up -d`
3. Wait until `docker compose ps` shows keycloak as **healthy** (can take several minutes the first time).
4. Check: open http://localhost:8080/health — it should show `"status": "UP"`.

---

## Step 2 — Prepare Keycloak (Local option only)

Docker imports the realm automatically, so skip this step for Docker.

For your own Keycloak on port 8086, check in the admin console (http://localhost:8086/admin), realm **BanfigoNew**:

1. **Clients → BanfigoFrontend → Settings → Capability config → Direct access grants: ON**, then **Save**.
   Postman logs in with username + password (the "password grant"), which needs this switch.
2. **Users** exist with these roles (or change the usernames/passwords in the environment, Step 4):

   | Username | Password | Realm role |
   |---|---|---|
   | `admin1` | `admin123` | ADMIN |
   | `maker1` | `maker123` | MAKER |
   | `checker1` | `checker123` | CHECKER |

   For each user: **Credentials → Set password** with *Temporary* **off**, and **Role mapping → Assign role** (filter by realm roles).

Tip: the easiest way to get exactly this setup is to import `keycloak/realm-export.json` (**Create realm → Browse → select the file**). If a `BanfigoNew` realm already exists, delete it first.

---

## Step 3 — Import into Postman

1. Open Postman → **Import** (top left).
2. Drag in all three `.json` files from this folder (or **files → select**), then **Import**.
3. You now have a collection **Banfigo API** and two environments **Banfigo – Local** and **Banfigo – Docker**.

---

## Step 4 — Select the environment

1. Top right, open the environment dropdown and choose **Banfigo – Local** or **Banfigo – Docker** (matching Step 1).
2. Optional: click the eye icon to check the values. If your users or passwords differ, edit
   `adminUser`, `adminPassword`, `makerUser`, `makerPassword`, `checkerUser`, `checkerPassword` and **Save**.

| Variable | Local | Docker |
|---|---|---|
| `baseUrl` | `http://localhost:8082` | `http://localhost:8080` |
| `keycloakUrl` | `http://localhost:8086` | `http://localhost:8080/auth` |
| `realm` | `BanfigoNew` | `BanfigoNew` |
| `clientId` | `BanfigoFrontend` | `BanfigoFrontend` |

---

## Step 5 — Check that tokens work

1. Open folder **0. Auth (manual check)** → **Get token – maker1** → **Send**.
2. Expected: **200 OK** with an `access_token`.
3. Optional: copy the `access_token`, paste it into https://jwt.io and look at:
   - `iss` → `http://localhost:8086/realms/BanfigoNew` (Local) or `http://localhost:8080/auth/realms/BanfigoNew` (Docker)
   - `realm_access.roles` → contains `MAKER`
   - `exp` → the token is valid for 5 minutes

You never need to copy tokens into requests: the collection's pre-request script logs in as admin1, maker1 and checker1
automatically and renews the tokens before they expire. Each request already says which user it runs as (Authorization tab).

If this step fails, see [Troubleshooting](#troubleshooting).

---

## Step 6 — Run the requests in order

Run the folders **top to bottom**: later requests use IDs saved by earlier ones (customer, account, beneficiary, consent).
Click a request → **Send** → check the status code and the **Test Results** tab.

| Folder | Runs as | What it does | Expected |
|---|---|---|---|
| **1. Health & Info** | no token | Health check and app info | 200 |
| **2. Customers** | admin1 (writes), maker1 (reads) | Create customers A and B, list, get, update | 201, 201, 200, 200, 200 |
| **3. Accounts** | admin1 (writes), checker1 (reads) | Open account A (customer A) and B (customer B), list, get | 201, 201, 200, 200 |
| **4. Transactions** | maker1 | Deposit 5000, withdraw 500, try to overdraw, history with paging/filter | 201, 201, **400**, 200, 200 |
| **5. Beneficiaries** | maker1 | Add a beneficiary for customer A, list | 201, 200 |
| **6. Transfers** | maker1 | A → B 250, pay beneficiary 100 | 201, 201 |
| **7. Statements** | maker1 | This month's statement as CSV and PDF | 200, 200 |
| **8. Dashboard** | checker1 | Totals, recent transactions, monthly flows | 200 |
| **9. Consents** | maker1 requests, checker1 decides | Request → maker can't approve (**403**) → checker approves → second consent rejected → approving it again fails (**409**) → first consent revoked | 201, 200, 200, **403**, 200, 201, 200, **409**, 200 |
| **10. Security & error checks** | various | 401 vs 403, validation, malformed JSON, wrong id type, not found, delete blocked | see below |
| **11. Cleanup** | checker1 | Delete the beneficiary | 204 |

Statements: to see the file, use **Save response → Save to a file** (PDF) or look at the body (CSV).

### Folder 10 explained — 401 vs 403 and error responses

| Request | Expected | Why |
|---|---|---|
| No token → 401 | **401 Unauthorized** | No JWT at all: the backend doesn't know who you are |
| Invalid token → 401 | **401 Unauthorized** | A JWT is sent, but its signature can't be verified |
| MAKER creates customer → 403 | **403 Forbidden** | Valid JWT (we know it's maker1), but creating customers needs **ADMIN** |
| MAKER opens account → 403 | **403 Forbidden** | Opening accounts needs **ADMIN** |
| CHECKER deposits → 403 | **403 Forbidden** | Transactions need **MAKER** |
| MAKER deletes beneficiary → 403 | **403 Forbidden** | Deleting beneficiaries needs **ADMIN** or **CHECKER** |
| Invalid customer body → 400 | **400 Bad Request** | `details` lists each invalid field |
| Malformed JSON → 400 | **400 Bad Request** | Body isn't valid JSON |
| Wrong id type → 400 | **400 Bad Request** | `/api/accounts/abc` — id must be a number |
| Unknown customer → 404 | **404 Not Found** | No customer with that id |
| Delete customer with accounts → 409 | **409 Conflict** | Customer A still has an account, a beneficiary and consents |

**Rule of thumb:** 401 = *who are you?* (missing/invalid token). 403 = *I know who you are, but you're not allowed* (wrong role).

---

## Step 7 — Run everything at once (Collection Runner)

1. Right-click the **Banfigo API** collection → **Run collection**.
2. Keep all requests ticked, in order. Iterations: **1**.
3. Click **Run Banfigo API**.
4. Expected: every test passes (green). You can run it again any time — account numbers are generated fresh on every run.

Save a screenshot of the result for your submission.

---

## Who can do what (role rules from `SecurityConfig.java`)

| Action | Needs |
|---|---|
| `GET /health`, `GET /api/info` | nothing (public) |
| Any other `GET` | any valid token |
| Create / update / delete customers | ADMIN |
| Open accounts | ADMIN |
| Deposits / withdrawals, transfers | MAKER |
| Add beneficiaries | any valid token |
| Delete beneficiaries | ADMIN or CHECKER |
| Request a consent | MAKER |
| Approve / reject / revoke a consent | ADMIN or CHECKER (and not the user who requested it) |

---

## Troubleshooting

| Problem | Cause → fix |
|---|---|
| Postman console: *Could not get a token … 401 invalid_grant* | Wrong username/password, or the user has a temporary password. Fix in Keycloak or in the environment. |
| *Could not get a token … 400 unauthorized_client* | **Direct access grants** is off for `BanfigoFrontend` (Step 2). |
| *Could not get a token … 404* | Wrong `keycloakUrl` or `realm` in the environment (Docker needs `/auth`). |
| Every request returns **401** | The token's issuer doesn't match the backend's `issuer-uri`. Local: backend expects Keycloak at `localhost:8086`. Docker: open everything through `localhost:8080`. |
| Requests return **403** where you expect 200 | The user is missing the realm role. Check **Users → Role mapping** in Keycloak. |
| `{{customerId}}` / `{{accountId}}` errors, or 404s on later folders | An earlier request didn't run or failed. Run the folders in order from folder 2. |
| *Could not send request* / connection refused | The backend (or Nginx for Docker) isn't running — repeat Step 1's check. |
| `Withdraw too much` returns 201 | Deposit amounts were changed; it only fails when the amount is above the balance. |

To see what the token script did: **View → Show Postman Console** (bottom left).
