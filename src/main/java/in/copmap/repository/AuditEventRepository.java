package in.copmap.repository;

import in.copmap.domain.AuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {
    Page<AuditEvent> findByEntityTypeAndEntityId(String entityType, UUID entityId, Pageable pageable);
}
