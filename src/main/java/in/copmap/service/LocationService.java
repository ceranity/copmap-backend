package in.copmap.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.copmap.config.RedisConfig;
import in.copmap.domain.LocationPing;
import in.copmap.exception.ApiException;
import in.copmap.repository.LocationPingRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Two-tier location store:
 *   - Every ping is persisted to Postgres for history/audit.
 *   - The latest ping per officer is also cached in Redis (key: copmap:live:{officerId})
 *     with a TTL so "stale" officers drop off the live map automatically.
 *   - A pub/sub message is emitted so WebSocket nodes can fan out to the planner UI.
 */
@Service
public class LocationService {

    private static final String LIVE_KEY_PREFIX = "copmap:live:";

    private final LocationPingRepository repo;
    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;
    private final Duration ttl;

    public LocationService(LocationPingRepository repo, StringRedisTemplate redis,
                           ObjectMapper mapper,
                           @org.springframework.beans.factory.annotation.Value("${copmap.location.live-ttl-seconds}") long ttlSeconds) {
        this.repo = repo; this.redis = redis; this.mapper = mapper;
        this.ttl = Duration.ofSeconds(ttlSeconds);
    }

    public LocationPing record(LocationPing ping) {
        if (ping.getRecordedAt() == null) ping.setRecordedAt(Instant.now());
        LocationPing saved = repo.save(ping);

        LiveLocation live = new LiveLocation(
                saved.getOfficerId(), saved.getLatitude(), saved.getLongitude(),
                saved.getAccuracyMeters(), saved.getSpeedMps(), saved.getBatteryPct(),
                saved.getRecordedAt());
        String json = toJson(live);
        redis.opsForValue().set(LIVE_KEY_PREFIX + saved.getOfficerId(), json, ttl);
        redis.convertAndSend(RedisConfig.CHANNEL_LOCATION, json);
        return saved;
    }

    public Optional<LiveLocation> getLive(UUID officerId) {
        String raw = redis.opsForValue().get(LIVE_KEY_PREFIX + officerId);
        return Optional.ofNullable(raw).map(this::fromJson);
    }

    public List<LiveLocation> getLiveForOfficers(Collection<UUID> officerIds) {
        if (officerIds.isEmpty()) return List.of();
        List<String> keys = officerIds.stream().map(id -> LIVE_KEY_PREFIX + id).collect(Collectors.toList());
        List<String> values = redis.opsForValue().multiGet(keys);
        if (values == null) return List.of();
        List<LiveLocation> out = new ArrayList<>();
        for (String v : values) if (v != null) out.add(fromJson(v));
        return out;
    }

    public List<LocationPing> history(UUID officerId, Instant from, Instant to) {
        return repo.history(officerId, from, to);
    }

    private String toJson(LiveLocation l) {
        try { return mapper.writeValueAsString(l); }
        catch (JsonProcessingException e) { throw ApiException.badRequest("Cannot serialize ping"); }
    }
    private LiveLocation fromJson(String s) {
        try { return mapper.readValue(s, LiveLocation.class); }
        catch (Exception e) { return null; }
    }
}
