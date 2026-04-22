package in.copmap.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.copmap.audit.AuditService;
import in.copmap.config.RedisConfig;
import in.copmap.domain.Alert;
import in.copmap.exception.ApiException;
import in.copmap.notification.NotificationService;
import in.copmap.repository.AlertRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class AlertService {

    private final AlertRepository repo;
    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;
    private final NotificationService notifications;
    private final AuditService audit;

    public AlertService(AlertRepository repo, StringRedisTemplate redis, ObjectMapper mapper,
                        NotificationService notifications, AuditService audit) {
        this.repo = repo; this.redis = redis; this.mapper = mapper;
        this.notifications = notifications; this.audit = audit;
    }

    @Transactional
    public Alert raise(Alert a) {
        Alert saved = repo.save(a);
        publish(saved);
        notifications.dispatch(saved);
        audit.record("ALERT_RAISED", "Alert", saved.getId(), a.getType() + " " + a.getSeverity());
        return saved;
    }

    public Page<Alert> listOpen(Pageable pageable) {
        return repo.findByStatus(Alert.AlertStatus.OPEN, pageable);
    }

    public Page<Alert> list(Pageable pageable) { return repo.findAll(pageable); }

    @Transactional
    public Alert acknowledge(UUID id, UUID userId) {
        Alert a = repo.findById(id).orElseThrow(() -> ApiException.notFound("Alert"));
        if (a.getStatus() == Alert.AlertStatus.RESOLVED) throw ApiException.badRequest("Already resolved");
        a.setStatus(Alert.AlertStatus.ACKNOWLEDGED);
        a.setAcknowledgedBy(userId);
        a.setAcknowledgedAt(Instant.now());
        audit.record("ALERT_ACK", "Alert", id, null);
        return a;
    }

    @Transactional
    public Alert resolve(UUID id) {
        Alert a = repo.findById(id).orElseThrow(() -> ApiException.notFound("Alert"));
        a.setStatus(Alert.AlertStatus.RESOLVED);
        audit.record("ALERT_RESOLVE", "Alert", id, null);
        return a;
    }

    private void publish(Alert a) {
        try { redis.convertAndSend(RedisConfig.CHANNEL_ALERTS, mapper.writeValueAsString(a)); }
        catch (JsonProcessingException ignored) { /* never fail the business op on telemetry */ }
    }
}
