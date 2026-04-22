package in.copmap.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "checkpoints")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Checkpoint {

    @Id @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patrol_id", nullable = false)
    private Patrol patrol;

    @Column(nullable = false)
    private int sequence;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    /** Acceptance radius in meters for marking a check-in as valid. */
    @Column(name = "radius_meters", nullable = false)
    private int radiusMeters;

    /** Minutes from patrol start by which officer should visit this point. */
    @Column(name = "due_offset_minutes")
    private Integer dueOffsetMinutes;
}
