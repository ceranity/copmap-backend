package in.copmap.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "patrols")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Patrol {

    @Id @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(name = "beat_name", length = 128)
    private String beatName;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private OperationStatus status;

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

    @OneToMany(mappedBy = "patrol", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sequence ASC")
    @Builder.Default
    private List<Checkpoint> checkpoints = new ArrayList<>();

    @PrePersist void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
        if (status == null) status = OperationStatus.PLANNED;
    }
}
