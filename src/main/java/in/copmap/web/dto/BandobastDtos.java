package in.copmap.web.dto;

import in.copmap.domain.Bandobast;
import in.copmap.domain.OperationStatus;
import jakarta.validation.constraints.*;

import java.time.Instant;
import java.util.UUID;

public class BandobastDtos {

    public record CreateReq(
            @NotNull Bandobast.OperationKind kind,
            @NotBlank String title,
            String description,
            String venue,
            double latitude,
            double longitude,
            @Min(10) @Max(10000) int radiusMeters,
            @NotNull Instant startAt,
            @NotNull Instant endAt,
            Integer expectedCrowd,
            String threatLevel,
            UUID stationId) {}

    public record Resp(
            UUID id, Bandobast.OperationKind kind, String title, String description,
            String venue, double latitude, double longitude, int radiusMeters,
            Instant startAt, Instant endAt, OperationStatus status,
            Integer expectedCrowd, String threatLevel,
            UUID createdBy, UUID stationId, Instant createdAt,
            Instant closedAt, String closureNotes) {
        public static Resp of(Bandobast b) {
            return new Resp(b.getId(), b.getKind(), b.getTitle(), b.getDescription(),
                    b.getVenue(), b.getLatitude(), b.getLongitude(), b.getRadiusMeters(),
                    b.getStartAt(), b.getEndAt(), b.getStatus(),
                    b.getExpectedCrowd(), b.getThreatLevel(),
                    b.getCreatedBy(), b.getStationId(), b.getCreatedAt(),
                    b.getClosedAt(), b.getClosureNotes());
        }
    }
}
