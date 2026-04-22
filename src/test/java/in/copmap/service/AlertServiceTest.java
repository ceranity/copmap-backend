package in.copmap.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.copmap.audit.AuditService;
import in.copmap.domain.Alert;
import in.copmap.exception.ApiException;
import in.copmap.notification.NotificationService;
import in.copmap.repository.AlertRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AlertServiceTest {

    AlertRepository repo;
    StringRedisTemplate redis;
    NotificationService notifications;
    AuditService audit;
    AlertService service;

    @BeforeEach
    void setUp() {
        repo = mock(AlertRepository.class);
        redis = mock(StringRedisTemplate.class);
        notifications = mock(NotificationService.class);
        audit = mock(AuditService.class);
        service = new AlertService(repo, redis, new ObjectMapper(), notifications, audit);
    }

    @Test
    void raiseSavesPublishesAndNotifies() {
        Alert a = Alert.builder()
                .type(Alert.AlertType.PANIC)
                .severity(Alert.Severity.CRITICAL)
                .officerId(UUID.randomUUID())
                .message("SOS")
                .build();
        when(repo.save(any(Alert.class))).thenAnswer(i -> {
            Alert v = i.getArgument(0);
            v.setId(UUID.randomUUID());
            return v;
        });

        Alert saved = service.raise(a);

        assertThat(saved.getId()).isNotNull();
        verify(redis).convertAndSend(eq("copmap.alerts"), anyString());
        verify(notifications).dispatch(saved);
        verify(audit).record(eq("ALERT_RAISED"), eq("Alert"), eq(saved.getId()), any());
    }

    @Test
    void acknowledgeSetsFields() {
        UUID id = UUID.randomUUID();
        UUID user = UUID.randomUUID();
        Alert a = Alert.builder().id(id).status(Alert.AlertStatus.OPEN).build();
        when(repo.findById(id)).thenReturn(Optional.of(a));

        Alert out = service.acknowledge(id, user);
        assertThat(out.getStatus()).isEqualTo(Alert.AlertStatus.ACKNOWLEDGED);
        assertThat(out.getAcknowledgedBy()).isEqualTo(user);
        assertThat(out.getAcknowledgedAt()).isNotNull();
    }

    @Test
    void acknowledgeRejectsWhenResolved() {
        UUID id = UUID.randomUUID();
        Alert a = Alert.builder().id(id).status(Alert.AlertStatus.RESOLVED).build();
        when(repo.findById(id)).thenReturn(Optional.of(a));

        assertThatThrownBy(() -> service.acknowledge(id, UUID.randomUUID()))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("resolved");
    }

    @Test
    void resolveTransitionsAndAudits() {
        UUID id = UUID.randomUUID();
        Alert a = Alert.builder().id(id).status(Alert.AlertStatus.ACKNOWLEDGED).build();
        when(repo.findById(id)).thenReturn(Optional.of(a));

        Alert out = service.resolve(id);
        assertThat(out.getStatus()).isEqualTo(Alert.AlertStatus.RESOLVED);
        verify(audit).record(eq("ALERT_RESOLVE"), eq("Alert"), eq(id), isNull());
    }

    @Test
    void acknowledgeMissingAlertThrowsNotFound() {
        UUID id = UUID.randomUUID();
        when(repo.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.acknowledge(id, UUID.randomUUID()))
                .isInstanceOf(ApiException.class);
    }
}
