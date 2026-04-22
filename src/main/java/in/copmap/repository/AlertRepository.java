package in.copmap.repository;

import in.copmap.domain.Alert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AlertRepository extends JpaRepository<Alert, UUID> {
    Page<Alert> findByStatus(Alert.AlertStatus status, Pageable pageable);
    Page<Alert> findByOfficerId(UUID officerId, Pageable pageable);
}
