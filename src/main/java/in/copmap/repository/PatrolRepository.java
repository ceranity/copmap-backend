package in.copmap.repository;

import in.copmap.domain.OperationStatus;
import in.copmap.domain.Patrol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PatrolRepository extends JpaRepository<Patrol, UUID> {
    List<Patrol> findByStatus(OperationStatus status);
    List<Patrol> findByStationId(UUID stationId);
}
