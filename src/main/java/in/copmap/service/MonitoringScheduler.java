package in.copmap.service;

import in.copmap.domain.Alert;
import in.copmap.repository.AssignmentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Background watchdog. For every officer currently CHECKED_IN to a patrol/bandobast,
 * verifies that a recent location ping exists in Redis. If not, raises a NO_LOCATION
 * alert exactly once per gap.
 *
 * Keeps state-of-silence in-memory (per-JVM) to avoid duplicate alerts on every tick.
 */
@Component
@Slf4j
public class MonitoringScheduler {

    private final AssignmentRepository assignments;
    private final LocationService locations;
    private final AlertService alerts;
    private final java.util.Set<UUID> silenced = java.util.concurrent.ConcurrentHashMap.newKeySet();

    public MonitoringScheduler(AssignmentRepository assignments, LocationService locations, AlertService alerts) {
        this.assignments = assignments; this.locations = locations; this.alerts = alerts;
    }

    @Scheduled(fixedDelay = 60_000, initialDelay = 60_000)
    public void scanForSilentOfficers() {
        assignments.findAll().stream()
                .filter(a -> a.getStatus() == in.copmap.domain.Assignment.AssignmentStatus.CHECKED_IN)
                .forEach(a -> {
                    UUID officer = a.getOfficerId();
                    if (locations.getLive(officer).isEmpty()) {
                        if (silenced.add(officer)) {
                            alerts.raise(Alert.builder()
                                    .type(Alert.AlertType.NO_LOCATION)
                                    .severity(Alert.Severity.WARNING)
                                    .officerId(officer)
                                    .patrolId(a.getPatrolId())
                                    .bandobastId(a.getBandobastId())
                                    .message("No location ping received from officer in live window")
                                    .build());
                        }
                    } else {
                        silenced.remove(officer);
                    }
                });
    }
}
