package in.copmap.web.dto;

import in.copmap.domain.Checkpoint;
import in.copmap.domain.OperationStatus;
import in.copmap.domain.Patrol;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class PatrolDtos {

    public record CheckpointReq(
            @Min(1) int sequence,
            @NotBlank String name,
            double latitude,
            double longitude,
            @Min(10) @Max(5000) int radiusMeters,
            Integer dueOffsetMinutes) {}

    public record CreatePatrolReq(
            @NotBlank String title,
            String description,
            String beatName,
            @NotNull Instant startAt,
            @NotNull Instant endAt,
            UUID stationId,
            @Valid @NotEmpty List<CheckpointReq> checkpoints) {}

    public record PatrolResp(
            UUID id, String title, String description, String beatName,
            Instant startAt, Instant endAt, OperationStatus status,
            UUID createdBy, UUID stationId, Instant createdAt,
            Instant closedAt, String closureNotes,
            List<CheckpointResp> checkpoints) {
        public static PatrolResp of(Patrol p) {
            return new PatrolResp(
                    p.getId(), p.getTitle(), p.getDescription(), p.getBeatName(),
                    p.getStartAt(), p.getEndAt(), p.getStatus(),
                    p.getCreatedBy(), p.getStationId(), p.getCreatedAt(),
                    p.getClosedAt(), p.getClosureNotes(),
                    p.getCheckpoints().stream().map(CheckpointResp::of).collect(Collectors.toList()));
        }
    }

    public record CheckpointResp(UUID id, int sequence, String name,
                                 double latitude, double longitude,
                                 int radiusMeters, Integer dueOffsetMinutes) {
        public static CheckpointResp of(Checkpoint c) {
            return new CheckpointResp(c.getId(), c.getSequence(), c.getName(),
                    c.getLatitude(), c.getLongitude(), c.getRadiusMeters(), c.getDueOffsetMinutes());
        }
    }

    public record AssignReq(@NotNull UUID officerId, String role) {}
    public record CloseReq(String notes) {}
}
