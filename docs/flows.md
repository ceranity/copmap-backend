# Operational flows

Authored by Krishnamurti.

## Patrol lifecycle

```mermaid
stateDiagram-v2
    [*] --> PLANNED: planner creates patrol + checkpoints
    PLANNED --> ACTIVE: planner clicks Start (officers already assigned)
    PLANNED --> CANCELLED: called off before start
    ACTIVE --> CLOSED: planner closes after shift
    ACTIVE --> CANCELLED: aborted mid-shift
    CLOSED --> [*]
    CANCELLED --> [*]
```

Every transition writes an AUDIT_EVENT row and is allowed only via the explicit `canTransition()` function inside the service — the REST layer exposes `/start`, `/close`, `/cancel` rather than a generic PATCH to avoid illegal jumps.

## Bandobast / Nakabandi lifecycle

Identical state machine, additional fields in the aggregate (kind, venue, cordon radius, expected_crowd, threat_level).

## Assignment lifecycle (officer-side)

```mermaid
stateDiagram-v2
    [*] --> ASSIGNED: planner assigns
    ASSIGNED --> ACKNOWLEDGED: officer acks notification
    ACKNOWLEDGED --> CHECKED_IN: officer reaches beat/venue
    CHECKED_IN --> CHECKED_OUT: officer ends shift
    ASSIGNED --> NO_SHOW: never acknowledged by shift end (future: scheduler)
    CHECKED_OUT --> [*]
```

## Live monitoring sequence

```mermaid
sequenceDiagram
    autonumber
    participant O as Officer App
    participant API as CopMap API
    participant PG as Postgres
    participant RD as Redis
    participant P as Planner UI

    O->>API: POST /api/locations/ping (JWT)
    API->>PG: INSERT location_pings
    API->>RD: SET copmap:live:{id} TTL 120s
    API->>RD: PUBLISH copmap.location
    RD-->>API: (other replicas subscribed)
    API->>P: STOMP /topic/locations
    Note over P: map marker updates
```

## PANIC flow

```mermaid
sequenceDiagram
    autonumber
    participant O as Officer App
    participant API as CopMap API
    participant PG as Postgres
    participant RD as Redis
    participant N as Notification channel (log/email/whatsapp-mock)
    participant P as Planner UI

    O->>API: POST /api/alerts (type=PANIC, severity=CRITICAL)
    API->>PG: INSERT alerts (status OPEN)
    API->>RD: PUBLISH copmap.alerts
    API->>N: dispatch (async)
    RD-->>P: STOMP /topic/alerts
    P->>API: POST /api/alerts/{id}/acknowledge
    API->>PG: UPDATE alerts SET status=ACKNOWLEDGED
    API->>PG: INSERT audit_events
```
