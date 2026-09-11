# REST–SOAP Integration Bridge with Sync Dashboard

Three cooperating services that simulate a real enterprise integration problem: a modern REST-based system, a legacy SOAP-based system, and a middleware **sync bridge** that keeps customer records consistent between them, detects and surfaces true conflicts, and exposes a React dashboard for monitoring sync history and manually resolving those conflicts.

## Architecture

```
[REST Service - "System A"]        [SOAP Service - "System B"]
   Spring Boot + REST API             Spring Boot + Spring-WS
   own DB, field names:                own DB, field names:
   {id, fullName, emailAddress,        {customerNumber, name, contactEmail,
    phoneNumber, updatedAt}             phone, lastModified}
          ^                                      ^
          |  REST calls                          |  SOAP/XML calls
          |                                      |
          +------------[Sync Bridge Service]-----+
                        - pulls from both
                        - maps schemas
                        - detects conflicts
                        - writes reconciled record back to both
                        - records SyncJob + SyncConflict history
                        - exposes dashboard API
                              |
                              v
                     [React Sync Dashboard]
```

The three backend modules are separate Spring Boot applications that only communicate over their public APIs (HTTP REST and SOAP/XML) — no shared database and no direct method calls. The field-name mismatch between System A and System B is intentional and forces a real schema-mapping layer.

## Modules

| Module | Port | Technology | Role |
|---|---|---|---|
| `system-a-rest` | 8091 | Spring Boot, Spring Web, Spring Data JPA, H2 | REST system ("System A") |
| `system-b-soap` | 8092 | Spring Boot, Spring-WS, JAXB, H2 | SOAP system ("System B") |
| `sync-bridge` | 8093 | Spring Boot, RestTemplate, WebServiceTemplate + JAXB, H2 | Sync/conflict middleware |
| `sync-dashboard` | 3000 | React 18, TypeScript, Vite 5, Tailwind CSS 3 | Monitoring + resolution UI |

## Prerequisites

- **Java 17+** (`java -version`)
- **Node.js 18+ and npm** (for the dashboard) (`node -v`)
- No global Maven install needed — the repo ships a wrapper (`mvnw` / `mvnw.cmd`) under the root.

## Running the services

> All commands below run from the repo root. On Windows use `mvnw.cmd`, on macOS/Linux use `./mvnw`.

### 1. Build everything

```bash
./mvnw clean package     # or: mvnw.cmd clean package
```

### 2. Start System A (REST) — port 8091

```bash
./mvnw -pl system-a-rest spring-boot:run
```

Verify: `curl http://localhost:8091/api/customers`

### 3. Start System B (SOAP) — port 8092

```bash
./mvnw -pl system-b-soap spring-boot:run
```

Verify: the WSDL is viewable at `http://localhost:8092/ws/customer.wsdl`.

### 4. Start the Sync Bridge — port 8093

```bash
./mvnw -pl sync-bridge spring-boot:run
```

Verify: `curl http://localhost:8093/api/sync/jobs`

### 5. Start the dashboard — port 3000

```bash
cd sync-dashboard
npm install
npm run dev
```

Open `http://localhost:3000`. The Vite dev server proxies `/api` to the bridge at `http://localhost:8093`, so no CORS setup is needed in development.

## Using the dashboard

The dashboard talks only to the Sync Bridge. A "Trigger Sync Now" button is on the **Sync Job History** page.

1. **Sync Job History** (`/`) — table of past sync jobs with status, counts, and duration, plus the manual trigger button.
2. **Job Detail** (`/jobs/:id`) — drill into one job's processed / conflicted / failed counts.
3. **Conflicts Inbox** (`/conflicts`) — lists `UNRESOLVED` conflicts, showing each field with both systems' values side by side. Choose "Keep A's value", "Keep B's value", or type a manual value. Resolving writes the winning value back to **both** systems immediately.
4. **Customer Records View** (`/records`) — all canonical mappings with their `IN_SYNC` / `CONFLICT` / `ERROR` status, searchable by name or email.

## Triggering a sync and resolving a conflict (API walkthrough)

### Manual sync trigger

```bash
curl -X POST http://localhost:8093/api/sync/trigger
```

### List sync jobs (most recent first)

```bash
curl http://localhost:8093/api/sync/jobs
```

### List unresolved conflicts

```bash
curl "http://localhost:8093/api/sync/conflicts?status=UNRESOLVED"
```

### Resolve a conflict

```bash
# Keep System A's value
curl -X POST http://localhost:8093/api/sync/conflicts/1/resolve \
  -H "Content-Type: application/json" \
  -d '{"resolution":"A_WINS"}'

# Keep System B's value
curl -X POST http://localhost:8093/api/sync/conflicts/1/resolve \
  -H "Content-Type: application/json" \
  -d '{"resolution":"B_WINS"}'

# Provide a manual value pushed to both systems
curl -X POST http://localhost:8093/api/sync/conflicts/1/resolve \
  -H "Content-Type: application/json" \
  -d '{"resolution":"MANUAL","manualValue":"New Value"}'
```

### List all canonical mappings

```bash
curl http://localhost:8093/api/sync/mappings
```

## How the sync works

A sync job runs automatically every **10 minutes** (`@Scheduled(fixedRate = 600000)` in `SyncService`) and can also be triggered manually.

1. Creates a `SyncJob` row with status `RUNNING`.
2. Pulls all customers from System A (`GET /api/customers`) and System B (`GetAllCustomers` SOAP operation).
3. Matches records by email (case-insensitive) and, for each existing mapping, compares `full_name`/`name`, `email_address`/`contactEmail`, and `phone_number`/`phone`:
   - **Both sides changed and now disagree** → a `SyncConflict` row is created as `UNRESOLVED`. Neither system is overwritten for a conflicted field.
   - **Only one side changed** → that value is propagated to the other system (a real write call), and the canonical fields + `last_synced_at` are updated.
   - **Neither changed** → nothing happens.
4. **Records only in A** are pushed to System B via `CreateCustomer`; **records only in B** are pushed to System A via `POST /api/customers` — both are linked in a new mapping.
5. The job is finalized with `records_processed`, `records_conflicted`, `records_failed`, and a status of `SUCCESS`, `PARTIAL`, or `FAILED`.

**Failure handling:** every downstream call (REST and SOAP) uses one automatic retry with a 500 ms delay before failing. A failed record is counted and processing continues; if either system is fully unreachable, the whole job is marked `FAILED` but the bridge service itself never crashes.

**Conflict resolution:** `A_WINS` / `B_WINS` push the winning value to the losing system; `MANUAL` requires a `manualValue` which is written to **both** systems. The canonical mapping is updated and its `sync_status` returns to `IN_SYNC` once no unresolved conflicts remain for that record.

## Blocking API endpoints

### System A (`system-a-rest`)

| Method | Path | Description |
|---|---|---|
| GET | `/api/customers` | list all, supports `?updatedSince=` |
| GET | `/api/customers/{id}` | single record |
| POST | `/api/customers` | create |
| PUT | `/api/customers/{id}` | update |

### System B (`system-b-soap`)

WSDL at `http://localhost:8092/ws/customer.wsdl`. Operations: `GetAllCustomers` (optional `lastModifiedSince`), `GetCustomerByNumber`, `CreateCustomer`, `UpdateCustomer`.

### Sync Bridge (`sync-bridge`)

| Method | Path | Description |
|---|---|---|
| POST | `/api/sync/trigger` | manually kick off a sync job |
| GET | `/api/sync/jobs` | list past sync jobs, most recent first |
| GET | `/api/sync/jobs/{id}` | detail of one job |
| GET | `/api/sync/conflicts?status=UNRESOLVED` | list conflicts needing attention |
| POST | `/api/sync/conflicts/{id}/resolve` | resolve with `{resolution, manualValue?}` |
| GET | `/api/sync/mappings` | list all canonical records + `sync_status` |

## Configuration

Service URLs for the bridge are configurable, never hardcoded (see `sync-bridge/src/main/resources/application.yml`):

```yaml
system-a:
  base-url: http://localhost:8091

system-b:
  base-url: http://localhost:8092

cors:
  allowed-origins: http://localhost:3000   # comma-separated list for production
```

The CORS policy allows requests from `cors.allowed-origins` (used if you deploy the dashboard separately instead of through the Vite proxy).

## Testing

Tests run with JUnit 5, Mockito, and WireMock — they do **not** require the other two services to be running.

```bash
./mvnw -pl sync-bridge test
```

| Test class | Count | What it covers |
|---|---|---|
| `ChangeDetectorTest` | 7 | pure field change-detection logic (no change, A-only, B-only, conflict, null handling) |
| `SyncServiceIntegrationTest` | 5 | clean sync, conflict detection without silent overwrite, simulated System B outage, one-sided propagation, conflict resolution with write-back to both systems |

A simulated outage test proves the bridge never crashes when a downstream system is unreachable.

## Design decisions worth knowing

- **Schema mapping:** email is the join key between the two systems (lowercased for matching). Fields map as `fullName↔name`, `emailAddress↔contactEmail`, `phoneNumber↔phone`.
- **SOAP client:** uses Spring-WS `WebServiceTemplate` with a `Jaxb2Marshaller` (explicit JAXB types in `com.integration.bridge.soap`) — no raw XML string building, no regex parsing.
- **New-record propagation:** as noted above, records unique to either side are pushed to the other side so the two systems converge.
- **Databases:** all three services use H2 in-memory databases seeded with overlapping sample customers so conflicts are easy to demo. Swap to MySQL/Postgres by changing each module's datasource in its `application.yml`.