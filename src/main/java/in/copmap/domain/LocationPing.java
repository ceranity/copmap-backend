package in.copmap.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Persisted officer GPS ping. The "live" view is served from Redis (last ping keyed by officer)
 * while history is served from this table. This two-tier pattern avoids hammering Postgres
 * for the map view while keeping an auditable trail.
 */
@Entity
@Table(name = "location_pings",
       indexes = {
           @Index(name = "ix_pings_officer_time", columnList = "officer_id,recorded_at")
       })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LocationPing {

    @Id @GeneratedValue
    private UUID id;

    @Column(name = "officer_id", nullable = false)
    private UUID officerId;

    @Column(name = "assignment_id")
    private UUID assignmentId;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    @Column(name = "accuracy_meters")
    private Double accuracyMeters;

    @Column(name = "speed_mps")
    private Double speedMps;

    @Column(name = "battery_pct")
    private Integer batteryPct;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;
}
