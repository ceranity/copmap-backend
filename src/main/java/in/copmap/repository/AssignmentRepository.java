package in.copmap.repository;

import in.copmap.domain.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AssignmentRepository extends JpaRepository<Assignment, UUID> {
    List<Assignment> findByOfficerId(UUID officerId);
    List<Assignment> findByPatrolId(UUID patrolId);
    List<Assignment> findByBandobastId(UUID bandobastId);
    boolean existsByOfficerIdAndPatrolId(UUID officerId, UUID patrolId);
    boolean existsByOfficerIdAndBandobastId(UUID officerId, UUID bandobastId);
}
