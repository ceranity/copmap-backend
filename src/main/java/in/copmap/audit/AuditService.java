package in.copmap.audit;

import in.copmap.domain.AuditEvent;
import in.copmap.repository.AuditEventRepository;
import in.copmap.security.UserPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Writes audit events in a REQUIRES_NEW transaction so a rollback on the business
 * operation doesn't erase the trail of the attempt (useful for "Cancelled by X" cases).
 */
@Service
public class AuditService {

    private final AuditEventRepository repo;

    public AuditService(AuditEventRepository repo) { this.repo = repo; }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String action, String entityType, UUID entityId, String details) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        UUID actorId = null;
        String actorName = null;
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal p) {
            actorId = p.id();
            actorName = p.username();
        }
        repo.save(AuditEvent.builder()
                .actorId(actorId)
                .actorUsername(actorName)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .details(details)
                .build());
    }
}
