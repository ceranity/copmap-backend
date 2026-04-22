# Entity-Relationship diagram

Authored by Krishnamurti.

```mermaid
erDiagram
    USERS ||--o{ ASSIGNMENTS        : "assigned to"
    USERS ||--o{ LOCATION_PINGS     : "emits"
    USERS ||--o{ ALERTS             : "raises"
    USERS ||--o{ AUDIT_EVENTS       : "acts as actor"
    PATROLS ||--o{ CHECKPOINTS      : "has"
    PATROLS ||--o{ ASSIGNMENTS      : "rosters"
    BANDOBAST ||--o{ ASSIGNMENTS    : "rosters"
    ASSIGNMENTS ||--o{ LOCATION_PINGS : "context"
    PATROLS ||--o{ ALERTS           : "raises from"
    BANDOBAST ||--o{ ALERTS         : "raises from"

    USERS {
      uuid id PK
      string username UK
      string password_hash
      string full_name
      string badge_number UK
      string phone
      enum role "OFFICER | PLANNER | SUPERVISOR"
      uuid station_id
      bool active
      timestamp created_at
    }
    PATROLS {
      uuid id PK
      string title
      string beat_name
      timestamp start_at
      timestamp end_at
      enum status "PLANNED|ACTIVE|CLOSED|CANCELLED"
      uuid created_by FK
      uuid station_id
      timestamp closed_at
      string closure_notes
    }
    CHECKPOINTS {
      uuid id PK
      uuid patrol_id FK
      int sequence
      string name
      float latitude
      float longitude
      int radius_meters
      int due_offset_minutes
    }
    BANDOBAST {
      uuid id PK
      enum kind "BANDOBAST|NAKABANDI"
      string title
      string venue
      float latitude
      float longitude
      int radius_meters
      timestamp start_at
      timestamp end_at
      enum status
      int expected_crowd
      string threat_level
      uuid created_by FK
    }
    ASSIGNMENTS {
      uuid id PK
      uuid officer_id FK
      uuid patrol_id FK "xor bandobast_id"
      uuid bandobast_id FK "xor patrol_id"
      string role
      enum status "ASSIGNED|ACKNOWLEDGED|CHECKED_IN|CHECKED_OUT|NO_SHOW"
      uuid assigned_by FK
      timestamp assigned_at
      timestamp acknowledged_at
      timestamp checked_in_at
      timestamp checked_out_at
    }
    LOCATION_PINGS {
      uuid id PK
      uuid officer_id FK
      uuid assignment_id FK
      float latitude
      float longitude
      float accuracy_meters
      float speed_mps
      int battery_pct
      timestamp recorded_at
    }
    ALERTS {
      uuid id PK
      enum type "PANIC|GEOFENCE_BREACH|MISSED_CHECKPOINT|NO_LOCATION|MANUAL"
      enum severity "INFO|WARNING|CRITICAL"
      uuid officer_id FK
      uuid patrol_id FK
      uuid bandobast_id FK
      string message
      float latitude
      float longitude
      enum status "OPEN|ACKNOWLEDGED|RESOLVED"
      uuid acknowledged_by FK
      timestamp acknowledged_at
      timestamp created_at
    }
    AUDIT_EVENTS {
      uuid id PK
      uuid actor_id FK
      string action
      string entity_type
      uuid entity_id
      string details
      timestamp at
    }
```

## Design notes

- **One assignment table, two foreign keys, XOR CHECK.** Keeps the "officer's current assignments" feed a single SELECT and gives audit a uniform row shape.
- **LOCATION_PINGS append-only**, PK on UUID, clustered read pattern on `(officer_id, recorded_at DESC)`. Redis carries the "latest" for the live map so this table is never read on the hot path.
- **AUDIT_EVENTS written in `REQUIRES_NEW`** so a rollback on the business operation doesn't remove the record that the attempt happened.
