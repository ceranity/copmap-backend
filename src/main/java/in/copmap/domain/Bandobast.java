package in.copmap.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Bandobast (event security deployment) and Nakabandi (vehicle-checking blockade)
 * share enough structure that we model them as one entity discriminated by {@link OperationKind}.
 */
@Entity
@Table(name = "bandobast")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Bandobast {

    @Id @GeneratedValue
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private OperationKind kind;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(length = 200)
    private String venue;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    /** Outer cordon radius in meters. */
    @Column(name = "radius_meters", nullable = false)
    private int radiusMeters;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private OperationStatus status;

    @Column(name = "expected_crowd")
    private Integer expectedCrowd;

    @Column(name = "threat_level", length = 16)
    private String threatLevel;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "station_id")
    private UUID stationId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "closure_notes", length = 2000)
    private String closureNotes;

    @PrePersist void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
        if (status == null) status = OperationStatus.PLANNED;
    }

    public enum OperationKind { BANDOBAST, NAKABANDI }
}
