# Project Specification: REST–SOAP Integration Bridge with Sync Dashboard

## 1. One-Line Description
Three cooperating services simulating a real enterprise integration scenario: a modern REST-based system, a legacy SOAP-based system, and a middleware "bridge" service that keeps customer records in sync between them, detects and surfaces conflicts when the same record changed in both places, and exposes a dashboard for monitoring sync history and manually resolving conflicts.

## 2. Why This Project Matters
Large organizations rarely run on one clean, unified system — they run on a patchwork of modern REST services and older systems that still speak SOAP, and a huge amount of real internal-engineering work is building and maintaining the middleware that keeps those systems consistent with each other. This project is a deliberately realistic simulation of that problem:
- It requires acting as a client to **both** a RESTful JSON API and a SOAP/XML API, which is explicitly a different (and less commonly practiced) skill than just building REST APIs.
- It requires designing a schema-mapping/transformation layer, since the two "systems" intentionally use different field names and shapes for the same real-world entity.
- It requires solving a genuine distributed-systems problem: what happens when the same record is edited in two places between syncs? Detecting and resolving that conflict is a core skill in systems integration work, not just plumbing data from A to B.
- It produces an operational dashboard — sync history, failure visibility, manual conflict resolution — which is what makes it a piece of *internal tooling* rather than just a backend exercise.

## 3. Tech Stack
- **All three services:** Java 17+, Spring Boot 3.x
- **REST Service ("System A"):** Spring Web, Spring Data JPA, its own MySQL or H2 database
- **SOAP Service ("System B"):** Spring-WS (`spring-ws-core`), XML schema (XSD) defining the SOAP contract, its own separate database
- **Sync Bridge Service (the core deliverable):** Spring Web (for its own dashboard API + triggering sync), `WebClient`/`RestTemplate` as a REST client, a SOAP client (JAX-WS generated stubs or Spring-WS `WebServiceTemplate`), Spring Scheduling, its own MySQL database for sync metadata
- **Frontend:** React (Vite), TypeScript, Tailwind CSS — talks only to the Sync Bridge Service's dashboard API
- **Testing:** JUnit 5 + Mockito, WireMock (to stub the REST/SOAP endpoints in bridge-service tests without needing the other two services running)

## 4. High-Level Architecture
```
[REST Service - "System A"]        [SOAP Service - "System B"]
   Spring Boot + REST API             Spring Boot + Spring-WS
   own DB, field names:                own DB, field names:
   {id, fullName, emailAddress,        {customerNumber, name, contactEmail,
    phoneNumber, updatedAt}             phone, lastModified}
          ^                                     ^
          |  REST calls                         |  SOAP/XML calls
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
Each of the three backend pieces should be its own separate Spring Boot application (three separate runnable projects/repos, or three modules in one multi-module Maven/Gradle project) so they genuinely communicate over the network like real independent systems, not just internal method calls.

## 5. Data Model

### System A (REST service) — `CustomerRecordA`
| Field | Type | Notes |
|---|---|---|
| id | BIGINT, PK | |
| full_name | VARCHAR(150) | |
| email_address | VARCHAR(150) | |
| phone_number | VARCHAR(30) | |
| updated_at | TIMESTAMP | updated on every change |

### System B (SOAP service) — `CustomerRecordB`
| Field | Type | Notes |
|---|---|---|
| customer_number | BIGINT, PK | deliberately different key name than System A |
| name | VARCHAR(150) | |
| contact_email | VARCHAR(150) | |
| phone | VARCHAR(30) | |
| last_modified | TIMESTAMP | |

The deliberate field-name mismatch between the two systems is intentional and required — it is what forces you to build a real mapping layer instead of a pass-through.

### Sync Bridge Service tables

**`CustomerRecordMapping`** (links the two systems' records to a canonical identity)
| Field | Type | Notes |
|---|---|---|
| id | BIGINT, PK | canonical id used by the bridge |
| system_a_id | BIGINT | nullable if not yet created in A |
| system_b_id | BIGINT | nullable if not yet created in B |
| canonical_name | VARCHAR(150) | last known reconciled value |
| canonical_email | VARCHAR(150) | |
| canonical_phone | VARCHAR(30) | |
| last_synced_at | TIMESTAMP | |
| sync_status | ENUM('IN_SYNC','CONFLICT','ERROR') | |

**`SyncJob`**
| Field | Type | Notes |
|---|---|---|
| id | BIGINT, PK | |
| started_at | TIMESTAMP | |
| completed_at | TIMESTAMP | nullable |
| status | ENUM('RUNNING','SUCCESS','FAILED','PARTIAL') | |
| records_processed | INT | |
| records_conflicted | INT | |
| records_failed | INT | |

**`SyncConflict`**
| Field | Type | Notes |
|---|---|---|
| id | BIGINT, PK | |
| sync_job_id | BIGINT, FK -> SyncJob.id | |
| mapping_id | BIGINT, FK -> CustomerRecordMapping.id | |
| field_name | VARCHAR(50) | which field disagreed, e.g. "email" |
| value_from_a | VARCHAR(255) | |
| value_from_b | VARCHAR(255) | |
| resolution | ENUM('UNRESOLVED','A_WINS','B_WINS','MANUAL') | default UNRESOLVED |
| resolved_by | VARCHAR(100) | nullable |
| resolved_at | TIMESTAMP | nullable |

## 6. API Endpoints

### System A (REST service)
| Method | Path | Description |
|---|---|---|
| GET | /api/customers | list all, supports `?updatedSince=` |
| GET | /api/customers/{id} | single record |
| POST | /api/customers | create |
| PUT | /api/customers/{id} | update |

### System B (SOAP service)
Exposed via a WSDL (auto-generated by Spring-WS from an XSD you define). Operations required:
- `GetAllCustomers` (optionally filtered by a `lastModifiedSince` element)
- `GetCustomerByNumber`
- `CreateCustomer`
- `UpdateCustomer`

### Sync Bridge Service (dashboard-facing API)
| Method | Path | Description |
|---|---|---|
| POST | /api/sync/trigger | manually kick off a sync job (also runs on a schedule) |
| GET | /api/sync/jobs | list past sync jobs, most recent first |
| GET | /api/sync/jobs/{id} | detail of one job |
| GET | /api/sync/conflicts?status=UNRESOLVED | list conflicts needing attention |
| POST | /api/sync/conflicts/{id}/resolve | body: `{ "resolution": "A_WINS" \| "B_WINS" \| "MANUAL", "manualValue": "optional string" }` |
| GET | /api/sync/mappings | list all canonical records and their current sync_status |

## 7. Core Business Logic (implement exactly as described)

**Sync algorithm (runs per scheduled job or manual trigger):**
1. Create a new `SyncJob` row with status RUNNING.
2. Fetch all records from System A (`GET /api/customers`) and all records from System B (`GetAllCustomers` SOAP call).
3. For each `CustomerRecordMapping` that already links an A-record and a B-record:
   - Compare `full_name`/`name`, `email_address`/`contact_email`, `phone_number`/`phone`.
   - For each field, if both sides changed since `last_synced_at` and now disagree, create a `SyncConflict` row for that field with status UNRESOLVED. Do not auto-overwrite either system for a conflicted field.
   - If only one side changed, propagate that value to the other system (call the appropriate update endpoint/SOAP operation) and update the canonical fields + `last_synced_at`.
   - If neither changed, do nothing.
4. For any record present in A but with no mapping, and not present by matching email in B: create a new mapping row, and optionally push it to B (your choice — document which behavior you implement).
5. On completion, update the `SyncJob` with `records_processed`, `records_conflicted`, `records_failed`, `completed_at`, and status (`SUCCESS` if no failures, `PARTIAL` if some records errored while others succeeded, `FAILED` if the whole job errored e.g. one system was unreachable).

**Conflict resolution:**
- When a user resolves a conflict via the dashboard with `A_WINS` or `B_WINS`, the bridge must immediately push the winning value to the losing system (a real write call, not just a database update on the bridge's own side) and update the `CustomerRecordMapping` canonical field + `last_synced_at`.
- `MANUAL` resolution requires a `manualValue` in the request body, which gets pushed to **both** systems.
- After resolution, `sync_status` on the mapping returns to `IN_SYNC` if no other conflicts remain open for that mapping.

**Failure handling:**
- If System A or System B is unreachable during a sync job (connection refused/timeout), catch the exception, mark that specific record's processing as failed (increment `records_failed`), and continue processing the remaining records rather than aborting the whole job.
- Implement one retry (a single retry attempt with a short delay) before marking a call as failed, using either manual retry logic or Spring Retry (`@Retryable`).

## 8. Frontend Pages (Sync Dashboard, talks only to the Bridge Service)
1. **Sync Job History** — table of past jobs with status, counts, duration; a "Trigger Sync Now" button
2. **Job Detail** — drill into one job's processed/conflicted/failed counts
3. **Conflicts Inbox** — list of UNRESOLVED conflicts, each showing the field name and both systems' values side by side, with buttons "Keep A's value" / "Keep B's value" / "Enter manual value"
4. **Customer Records View** — list of all canonical mappings with their current sync_status (IN_SYNC / CONFLICT / ERROR), searchable by name/email

## 9. Development Phases (build in this exact order)

**Phase 0 — Setup**
- Scaffold three separate Spring Boot projects: `system-a-rest`, `system-b-soap`, `sync-bridge`.
- Set up three separate databases (can be three schemas in one local MySQL instance).

**Phase 1 — Build System A (REST)**
- Implement `CustomerRecordA` entity and the four REST endpoints in section 6.
- Seed with a handful of sample customers.

**Phase 2 — Build System B (SOAP)**
- Define the XSD schema for customer records (using System B's field names, per section 5).
- Implement the four SOAP operations using Spring-WS.
- Seed with sample customers (some overlapping in identity with System A's data, some unique to B).
- Verify the WSDL is browsable and the service responds correctly to a raw SOAP request (test with Postman or `curl` before writing any client code).

**Phase 3 — Bridge service: read-only sync**
- Implement `CustomerRecordMapping`, `SyncJob` entities.
- Build the REST client (WebClient/RestTemplate) to pull from System A.
- Build the SOAP client (generate stubs from the WSDL, or use `WebServiceTemplate` with marshalling) to pull from System B.
- Implement a first version of the sync algorithm that only reads from both and creates/updates mappings — no conflict detection yet, no write-back yet.

**Phase 4 — Conflict detection and write-back**
- Extend the sync algorithm to implement the full comparison logic from section 7, including `SyncConflict` creation and one-sided propagation.
- Implement the conflict resolution endpoint with real write-back calls to both systems.

**Phase 5 — Failure handling and scheduling**
- Add the retry-then-fail behavior described in section 7.
- Add `@Scheduled` to run sync automatically (e.g. every 10 minutes) in addition to the manual trigger endpoint.

**Phase 6 — Dashboard + testing**
- Build all four frontend pages listed in section 8.
- Write unit tests for the field-comparison/conflict-detection logic in isolation (pure logic, no network calls).
- Write integration tests for the bridge service using WireMock to stub both System A's REST responses and System B's SOAP responses, covering: clean sync, a conflicting field, and a simulated unreachable system.
- Write the README covering how to run all three services together and a walkthrough of triggering a sync and resolving a conflict.

## 10. Non-Functional Requirements
- Same secret/config management conventions as the other two projects (no hardcoded URLs where possible — service base URLs for A and B should be configurable in the bridge service's config).
- The bridge service must never crash entirely because one of the two systems is down — this must be demonstrably true via a test.
- All XML/SOAP marshalling errors should be caught and logged clearly, not surfaced as generic 500s.

## 11. Stretch Goals (optional)
- Add a third heterogeneous system (e.g. a CSV-file-based "legacy export" system) to show the bridge pattern generalizes beyond just two systems.
- Add field-level sync history so you can see how a specific field's value changed over time across both systems.
- Add a "dry run" mode to sync that reports what *would* change without writing anything.

## 12. Definition of Done
- All three services run independently and communicate only over their public APIs (REST and SOAP respectively) — no shared database or direct method calls between them.
- Triggering a sync correctly propagates one-sided changes and correctly flags true conflicts without silently overwriting either system.
- A conflict can be resolved from the dashboard and the resolution is verifiably written back to both systems.
- A simulated outage of either System A or System B does not crash the bridge service or the sync job as a whole.
- Core sync/conflict logic is covered by automated tests that don't require all three services running simultaneously.