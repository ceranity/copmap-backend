package in.copmap.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Ties an officer to a patrol OR a bandobast (exactly one of the FK columns is set).
 * Modeled as a single table so the assignment feed and audit can be queried uniformly.
 */
@Entity
@Table(name = "assignments",
       indexes = {
           @Index(name = "ix_assignments_officer", columnList = "officer_id"),
           @Index(name = "ix_assignments_patrol", columnList = "patrol_id"),
           @Index(name = "ix_assignments_bandobast", columnList = "bandobast_id")
       })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Assignment {

    @Id @GeneratedValue
    private UUID id;

    @Column(name = "officer_id", nullable = false)
    private UUID officerId;

    @Column(name = "patrol_id")
    private UUID patrolId;

    @Column(name = "bandobast_id")
    private UUID bandobastId;

    @Column(length = 64)
    private String role;            // e.g. "BEAT_OFFICER", "NAKA_LEAD", "RESERVE"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AssignmentStatus status;

    @Column(name = "assigned_by", nullable = false)
    private UUID assignedBy;

    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;

    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt;

    @Column(name = "checked_in_at")
    private Instant checkedInAt;

    @Column(name = "checked_out_at")
    private Instant checkedOutAt;

    @PrePersist void onCreate() {
        if (assignedAt == null) assignedAt = Instant.now();
        if (status == null) status = AssignmentStatus.ASSIGNED;
    }

    public enum AssignmentStatus {
        ASSIGNED, ACKNOWLEDGED, CHECKED_IN, CHECKED_OUT, NO_SHOW
    }
}
