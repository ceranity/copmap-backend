package in.copmap.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_events",
       indexes = {
           @Index(name = "ix_audit_entity", columnList = "entity_type,entity_id"),
           @Index(name = "ix_audit_at", columnList = "at")
       })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditEvent {

    @Id @GeneratedValue
    private UUID id;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(name = "actor_username", length = 64)
    private String actorUsername;

    @Column(nullable = false, length = 64)
    private String action;

    @Column(name = "entity_type", nullable = false, length = 64)
    private String entityType;

    @Column(name = "entity_id")
    private UUID entityId;

    @Column(length = 2000)
    private String details;

    @Column(nullable = false)
    private Instant at;

    @PrePersist void onCreate() { if (at == null) at = Instant.now(); }
}
