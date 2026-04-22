package in.copmap.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "alerts",
       indexes = { @Index(name = "ix_alerts_created", columnList = "created_at") })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Alert {

    @Id @GeneratedValue
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AlertType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Severity severity;

    @Column(name = "officer_id")
    private UUID officerId;

    @Column(name = "patrol_id")
    private UUID patrolId;

    @Column(name = "bandobast_id")
    private UUID bandobastId;

    @Column(length = 500)
    private String message;

    private Double latitude;
    private Double longitude;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AlertStatus status;

    @Column(name = "acknowledged_by")
    private UUID acknowledgedBy;

    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
        if (status == null) status = AlertStatus.OPEN;
        if (severity == null) severity = Severity.INFO;
    }

    public enum AlertType {
        PANIC,                  // officer-triggered SOS
        GEOFENCE_BREACH,        // left assigned zone
        MISSED_CHECKPOINT,      // past due offset without check-in
        NO_LOCATION,            // device went silent
        MANUAL                  // planner-raised
    }

    public enum Severity { INFO, WARNING, CRITICAL }
    public enum AlertStatus { OPEN, ACKNOWLEDGED, RESOLVED }
}
