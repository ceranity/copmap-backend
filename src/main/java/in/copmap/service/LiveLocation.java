package in.copmap.service;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

public record LiveLocation(UUID officerId, double latitude, double longitude,
                           Double accuracyMeters, Double speedMps, Integer batteryPct,
                           Instant recordedAt) implements Serializable {}
