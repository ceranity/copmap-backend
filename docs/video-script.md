# Explanation video script (~8 minutes)

Authored by Krishnamurti.

> Target length: 8 minutes. Written as spoken prose — read at a natural pace.
> Timestamps are rough targets, not strict cuts. Screen shares in **[brackets]**.

---

## 0:00 — 0:30 · Intro

Hi, I'm Krishnamurti. This is my submission for the CopMap backend screening task. In the next eight minutes I'll walk through how I interpreted the problem, the architecture I landed on, the data model, one end-to-end demo, and the trade-offs I consciously made.

---

## 0:30 — 1:30 · Problem understanding

**[Screen: README §1]**

The brief asks for two operations: patrolling, and bandobast / nakabandi. I treated these as real police workflows rather than CRUD screens.

- **Patrolling** is a scheduled beat along a known route, with ordered checkpoints. A Station House Officer plans it, constables walk it, and the patrol is closed with a report.
- **Bandobast** is event-security — festivals, VIP movement, protests — with a large roster at a fixed location and an outer cordon radius.
- **Nakabandi** is a vehicle-checking blockade — highway or border — smaller team, specific roles like stopper, searcher, reserve.

The key insight is that all three share the same lifecycle: plan, assign, activate, monitor, close. So I modelled them with a common state machine while keeping the shape differences — a patrol has a route, a bandobast has a point-plus-radius.

Three actors: **Officer** on the ground, **Planner** (SHO) who creates and assigns, **Supervisor** (DCP-level) for oversight and audit.

---

## 1:30 — 3:00 · Architecture

**[Screen: docs/architecture.md — Mermaid diagram]**

Stack is Spring Boot 3.2, Postgres, Redis, WebSocket over STOMP, JWT for auth.

I drew six bounded contexts: **Identity, Planning, Field Telemetry, Incident, Reporting, Audit.** Today they're deployed as a modular monolith — each in its own package with clean service boundaries — because the brief values clarity over a service mesh. If traffic grows, the split is mechanical: Field Telemetry goes first because its scaling curve is different from the rest.

**[Screen: architecture sequence diagram]**

Three things are worth highlighting about the design:

**One — Redis carries the live state.** Every GPS ping is written to Postgres for history, and the latest ping per officer is also cached in Redis under `copmap:live:{officerId}` with a 2-minute TTL. The planner's map reads from Redis — milliseconds, no pressure on Postgres — and if an officer goes silent, the key expires and the officer disappears from the map for free.

**Two — Redis pub/sub fans out to multiple app replicas.** When a ping arrives, the service publishes to a Redis channel. Every replica subscribes and pushes over STOMP to its locally-connected WebSocket clients. That means I can run this behind a plain load balancer with no sticky-session requirement on the publisher side.

**Three — audit writes in a separate transaction.** `AuditService` uses `REQUIRES_NEW`, so even if a business operation rolls back, the audit row for the attempt survives. That matters for "who tried to cancel this operation" questions.

---

## 3:00 — 4:00 · Data model

**[Screen: docs/er-diagram.md]**

Seven tables. Two things I'd call attention to:

**Assignments is one table, not two.** It has an officer ID plus *either* a `patrol_id` *or* a `bandobast_id`, enforced with an XOR check constraint. This keeps "show me everything this officer is on" a single query, and the audit shape is uniform. The cost is one CHECK constraint — worth it.

**Bandobast and nakabandi share one table** discriminated by a `kind` enum, because their shape is identical — location, cordon, window, roster, status. The enum drives UI labels and role vocabularies. I could have split them; I didn't, because the cost of duplication outweighed the benefit.

Location pings are append-only, indexed on `(officer_id, recorded_at DESC)` — the only read pattern that matters. The live-map path never touches this table.

---

## 4:00 — 6:30 · Live demo

**[Screen: terminal]**

Let me show this working end-to-end.

```
docker compose up --build
```

**[Screen: Swagger UI → /swagger-ui.html]**

Every endpoint is documented here via OpenAPI. But I'll drive the actual demo through Postman because the collection chains the variables automatically.

**[Screen: Postman, collection imported]**

Four seeded users: supervisor, planner, officer1, officer2. All with password `copmap123`.

1. **Login as planner.** Token is captured into a collection variable automatically.
2. **List officers.** Grabs officer1's ID into another variable.
3. **Create a patrol** — title, beat, time window, two checkpoints. Response shows status PLANNED.
4. **Assign officer1** to the patrol with role BEAT_OFFICER.
5. **Start the patrol** — transitions to ACTIVE. An audit row is written.
6. **Login as officer1.** Separate token variable.
7. **Acknowledge the assignment, then check in.**
8. **Send a GPS ping.** Behind the scenes: Postgres INSERT, Redis SET with TTL, Redis PUBLISH.
9. **Switch back to planner.** **GET `/api/locations/live`** — officer1's position appears. That read hit Redis, not Postgres.
10. **Raise a PANIC alert as the officer.** Severity CRITICAL.
11. **As planner, list open alerts** — there it is. Acknowledge, then resolve. Each transition writes audit.
12. **Close the patrol with closure notes.**
13. **GET `/api/patrols/{id}/report`** — downloads a PDF with the roster, checkpoints, and closure notes. Same pattern for bandobast — the endpoint is `/plan` because it's the pre-operation briefing document.

In parallel, if I'd had a WebSocket client connected to `/topic/locations` and `/topic/alerts`, every ping and every alert would have arrived there in real time, published through Redis.

---

## 6:30 — 7:30 · Trade-offs and what I skipped

**[Screen: README §4 and §7]**

Four deliberate calls I want to call out:

**Modular monolith over microservices.** The bounded contexts are drawn in packages, not processes. Splitting is a refactor, not a rewrite.

**Redis + Postgres, not PostGIS.** At station scale, haversine in Java plus a B-tree on `(officer_id, recorded_at)` is fast enough. PostGIS is the upgrade when the query pattern becomes spatial-heavy — I'd flip when planners want "every officer within 500m of this alert" as a common query.

**JWT, stateless.** Revocation needs a Redis blacklist — documented as future work, not wired. Fast horizontal scaling was the bigger win for now.

**Skipped real push providers — FCM, Twilio, Gupshup.** The notification port is in place; only adapters are missing. I didn't want to fake a trial account for a screening task.

**Skipped Testcontainers-based integration tests.** The invariants that can break quietly — state machine, ownership check, geometry — are covered by unit tests. Full-context tests would need Postgres + Redis containers, which is over-scope for what the brief asks.

---

## 7:30 — 8:00 · Close

**[Screen: README]**

The repo has the README, the three diagram docs — architecture, ER, and operational flows — the Postman collection, the Dockerfile, and `docker compose up --build` takes it from zero to a running system.

Thanks for your time — I enjoyed thinking about this problem. Looking forward to the next conversation.

---

## Recording tips

- Tools that work on Linux: **OBS Studio** (full featured), **SimpleScreenRecorder** (lightweight), **Kazam**. Any of them at 1080p/30fps + a USB mic is fine.
- Read the script once silently, once aloud, then record. Don't try to memorise.
- Keep Postman pre-populated — don't type JSON bodies on camera.
- Pre-start `docker compose up` in another terminal so the app is warm before you hit record.
- 8 minutes is generous. If you run long, cut the trade-offs section to 45 seconds — the code is the product.
