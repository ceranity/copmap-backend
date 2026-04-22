package in.copmap.web.dto;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public class LocationDtos {
    public record PingReq(
            @NotNull Double latitude,
            @NotNull Double longitude,
            Double accuracyMeters,
            Double speedMps,
            Integer batteryPct,
            Instant recordedAt,
            UUID assignmentId) {}
}
