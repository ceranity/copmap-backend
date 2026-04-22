package in.copmap.web.dto;

import in.copmap.domain.Alert;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class AlertDtos {
    public record RaiseReq(
            @NotNull Alert.AlertType type,
            Alert.Severity severity,
            UUID officerId,
            UUID patrolId,
            UUID bandobastId,
            String message,
            Double latitude,
            Double longitude) {}
}
