# CopMap Backend

**Screening task submission — Patrolling + Bandobast / Nakabandi.**
Authored by **Krishnamurti**.

---

## 1. Problem understanding

Indian police forces run two distinct field operations that this system has to support as *workflows*, not mere CRUD screens:

- **Patrolling** — a scheduled beat along a known route with ordered checkpoints. A Station House Officer (SHO) or beat planner defines the beat (start/end window, checkpoints, acceptance radii). Constables are assigned, go on the ground, check in at each point, and the patrol is closed with a report. Value for the department: deterrence by presence, early incident detection, accountable coverage.
- **Bandobast / Nakabandi** — event or intelligence-driven deployment at a **fixed location** with an outer cordon radius:
  - *Bandobast* — security for an event (festival, VIP movement, protest). Large rosters, crowd estimate, threat classification.
  - *Nakabandi* — a vehicle-checking blockade (late-night/highway/border). Smaller teams, specific roles (lead, stopper, searcher, reserve).

Both share the same lifecycle: **PLAN → ASSIGN → ACTIVATE → MONITOR (live map + alerts) → CLOSE (report + audit)**. This project models them with a common state machine while keeping their distinguishing fields (route vs. point-with-cordon).

### Actors

| Role | What they do |
|---|---|
| **Supervisor** (DCP/ACP) | Oversight across stations. Read all operations, audit trail, escalate. |
| **Planner** (SHO / Station Officer) | Creates patrols/bandobast, assigns officers, monitors live map, closes operations. |
| **Officer** (Constable) | Receives assignments, acknowledges, checks in/out, sends GPS pings, raises PANIC alerts. |

Auth: username+password → JWT bearer token → stateless REST. Role-based access enforced at controller level via `@PreAuthorize`. Officers can act only on their own assignments (ownership check in service layer).

---

## 2. Architecture

```
┌─────────────┐    REST + STOMP/WebSocket     ┌────────────────────────────────────┐
│  Frontends  │ ─────────────────────────────▶│        Spring Boot service         │
│  (web/app)  │                               │                                    │
└─────────────┘                               │  Controllers → Services → JPA      │
                                              │                                    │
                                              │  NotificationService (log/        │
                                              │   email/mock-whatsapp)             │
                                              │                                    │
                                              │  MonitoringScheduler (stale GPS)   │
                                              └──────┬───────────────┬─────────────┘
                                                     │               │
                                          ┌──────────▼──────┐  ┌─────▼──────────┐
                                          │   PostgreSQL     │  │     Redis      │
                                          │  (source of     │  │  live:{officer}│
                                          │   truth, audit) │  │  pub/sub: loc. │
                                          └─────────────────┘  │  pub/sub: alrt.│
                                                               └────────────────┘
```

### Service boundaries (logical — deployed as one service for the screening scope)

| Bounded context | Responsibility |
|---|---|
| **Identity** | users, roles, auth, JWT issuance. `UserService`, `AuthController`. |
| **Planning** | patrol + bandobast CRUD, assignments, state transitions. `PatrolService`, `BandobastService`, `AssignmentService`. |
| **Field telemetry** | GPS ingestion, live state in Redis, history in Postgres. `LocationService`. |
| **Incident** | alert lifecycle, notification fan-out. `AlertService`, `NotificationService`. |
| **Reporting** | PDF plans + closure reports. `PdfService`. |
| **Audit** | append-only event log, own transaction scope. `AuditService`. |

Why a modular monolith now: the screening brief asks for clarity over sprawl. Boundaries are drawn cleanly in packages (`service/`, `web/`, `domain/`) so any context can be lifted into a separate service without reshaping callers. A realistic v2 split would put Field telemetry into its own service first (highest write volume, different scaling curve).

### Real-time mechanism

- **Officer → server**: HTTP POST for GPS pings (simple, proxy/firewall-friendly for mobile data).
- **Server → planner UI**: STOMP over WebSocket on `/ws`, topics `/topic/locations` and `/topic/alerts`.
- **Between app replicas**: Redis pub/sub. `LocationService`/`AlertService` publish to `copmap.location`/`copmap.alerts`. `RealtimeBridge` subscribes and fans out to locally-connected STOMP clients. This makes the WebSocket layer horizontally scalable behind a plain L4 load balancer without sticky sessions for the publisher, and `broadcast`-style sticky for subscribers (or a shared-broker upgrade later).

### Redis usage — and why

| Use | Key | Why Redis |
|---|---|---|
| Live officer position | `copmap:live:{officerId}` with TTL 120s | Map view needs last-known-position reads at read-mostly, millisecond latency. TTL gives "went silent" for free. |
| Pub/sub fan-out | channels `copmap.location`, `copmap.alerts` | Decouples writers from WebSocket sessions and scales across replicas. |
| (Future) rate-limit on PANIC | `copmap:rl:{officer}` | One-line addition with `INCR` + `EXPIRE`. Documented, not yet wired. |

### API contracts

REST under `/api/*`. Full OpenAPI served at `/swagger-ui.html`, spec JSON at `/v3/api-docs`. Postman collection at [`postman/CopMap.postman_collection.json`](postman/CopMap.postman_collection.json) chains login → create patrol → assign → start → ping → alert → close → download PDF.

---

## 3. Data design

See [`docs/er-diagram.md`](docs/er-diagram.md) for the Mermaid ER diagram.

Highlights:

- `assignments` unifies patrol and bandobast rosters in one table so "officer feed" and audit are single-query. A `CHECK` constraint enforces that exactly one of `patrol_id` / `bandobast_id` is non-null.
- `location_pings` is append-only, indexed by `(officer_id, recorded_at DESC)` — optimised for both live-tail and history queries.
- `bandobast` covers both `BANDOBAST` and `NAKABANDI` via an enum `kind` column. The shared shape (location + cordon + window + roster + status) is identical; the kind drives UI labels and roster-role vocabularies.
- `audit_events` is written in a `REQUIRES_NEW` transaction so a business-operation rollback doesn't erase the attempt trail. Indexed by `(entity_type, entity_id)` for drill-down.
- Migrations live in `src/main/resources/db/migration/V1__init.sql` (Flyway). Seed users are created by `DataSeeder` on first boot with properly hashed passwords.

---

## 4. Implementation status

### Implemented (end-to-end)

- Auth: login, registration, JWT with role claim, BCrypt passwords, method-level RBAC.
- Full CRUD + lifecycle (`PLANNED → ACTIVE → CLOSED/CANCELLED`) for **patrols** and **bandobast/nakabandi**, with state-machine enforcement and audit events at every transition.
- Assignments with per-officer ownership checks, ACK / check-in / check-out.
- Location ingestion: Postgres history + Redis live cache with TTL + pub/sub fan-out.
- WebSocket (STOMP) real-time endpoints: `/topic/locations`, `/topic/alerts`.
- Alerts: raise, list, acknowledge, resolve. Background `MonitoringScheduler` raises `NO_LOCATION` alerts when a CHECKED_IN officer goes silent, deduped in-memory.
- PDF generation: bandobast deployment plan, patrol closure report (OpenPDF).
- Notifications: pluggable channel (`log` / `email` / `mock-whatsapp`) — default `log` so CI and reviewers see output without SMTP setup.
- Audit trail for every operational action.
- OpenAPI / Swagger UI + Postman collection.
- Dockerfile (multi-stage) + docker-compose (Postgres + Redis + app).
- Unit tests for state machine, ownership check, and geo util.

### Consciously skipped (with reasoning)

- **Separate microservices.** Scope is "design + implement" not "operate a mesh". Boundaries are clean in-package so the split is a mechanical refactor.
- **Real push providers (FCM, Twilio).** The notification port is in place; only adapters are missing. Added cost without added insight for a screening task.
- **Testcontainers + full integration tests.** Pure unit tests cover the tricky invariants (state machine, ownership, geometry). Boot tests would need Testcontainers for Postgres + Redis, which is over-scope.
- **Offline ping buffering on device.** A real deployment needs the mobile app to queue pings when connectivity dips. Server accepts `recordedAt` in the payload so backfill works once that's built client-side.
- **Geofence breach detection with PostGIS.** Haversine util is included and good enough at station scale; PostGIS + spatial indexes become worthwhile at tens of thousands of pings/second.

---

## 5. Run it

### With Docker (recommended)

```bash
cd copmap-backend
docker compose up --build
```

Once `copmap-app` logs `Started CopMapApplication`, browse:

- Swagger UI → <http://localhost:8080/swagger-ui.html>
- Health → <http://localhost:8080/actuator/health>

### Without Docker (local dev)

Requires JDK 17, Maven 3.9+, a local Postgres and Redis:

```bash
createdb copmap
mvn spring-boot:run
```

### Seeded credentials

| Username | Password | Role |
|---|---|---|
| `supervisor` | `copmap123` | SUPERVISOR |
| `planner` | `copmap123` | PLANNER |
| `officer1` | `copmap123` | OFFICER |
| `officer2` | `copmap123` | OFFICER |

### End-to-end demo (via Postman)

1. Import `postman/CopMap.postman_collection.json`.
2. Run `Auth → Login (planner)` then `Auth → Login (officer1)` (tokens auto-saved).
3. `Users → List officers` (auto-saves `officerId`).
4. `Patrols → Create patrol` → `Assign officer` → `Start patrol`.
5. `Officer actions → Acknowledge → Check-in → Send GPS ping`.
6. `Live monitoring → Live locations` — the ping appears.
7. `Alerts → Raise alert (officer PANIC)` → `Acknowledge → Resolve`.
8. `Patrols → Close patrol` → `Download report (PDF)`.

### Tests

```bash
mvn test
```

---

## 6. Repository layout

```
copmap-backend/
├── Dockerfile
├── docker-compose.yml
├── pom.xml
├── README.md
├── docs/                 ← architecture, ER, flow diagrams
├── postman/              ← Postman collection
└── src/
    ├── main/java/in/copmap/
    │   ├── CopMapApplication.java
    │   ├── config/       ← Security, Redis, WebSocket, OpenAPI, DataSeeder
    │   ├── security/     ← JWT filter, principal, UserDetailsService
    │   ├── domain/       ← JPA entities + enums
    │   ├── repository/   ← Spring Data repositories
    │   ├── service/      ← business logic (Patrol, Bandobast, Assignment, Location, Alert)
    │   ├── web/          ← REST controllers + DTOs
    │   ├── websocket/    ← Redis → STOMP bridge
    │   ├── notification/ ← channel-pluggable dispatcher
    │   ├── pdf/          ← OpenPDF plan/report generator
    │   ├── audit/        ← audit log writer
    │   └── exception/    ← ApiException + @RestControllerAdvice
    └── main/resources/
        ├── application.yml
        ├── application-test.yml
        └── db/migration/V1__init.sql
```

---

## 7. Trade-offs (short form)

- **Monolith now, boundaries drawn for split later.** Saves weeks of operational cost; the refactor is mechanical.
- **Postgres + Redis instead of PostGIS.** Adequate at station scale; revisit if the query pattern becomes spatial-heavy.
- **JWT (stateless) over sessions.** Horizontal scaling is free; revocation needs a blacklist cache (trivially a Redis set) — not implemented yet.
- **One table for patrol+bandobast assignments.** Single feed query, one audit shape. Costs a CHECK constraint; worth it.
- **REST + STOMP instead of MQTT.** Matches what Spring Boot + browser frontends do well out of the box; MQTT is the upgrade path when the device fleet grows.

---

## 8. Submission checklist (per the screening brief)

- [x] Public GitHub repo (push `copmap-backend/` as the repo root)
- [x] README (this file) — problem understanding, architecture, trade-offs, implemented vs skipped
- [x] Diagrams — see [`docs/`](docs/)
- [x] Postman collection — [`postman/CopMap.postman_collection.json`](postman/CopMap.postman_collection.json)
- [x] Docker run instructions — section 5
- [ ] 5–10 min explanation video — record separately, link in the submission email
- [ ] Resume (PDF) — attach to submission email
