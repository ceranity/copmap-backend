package in.copmap.repository;

import in.copmap.domain.Bandobast;
import in.copmap.domain.OperationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BandobastRepository extends JpaRepository<Bandobast, UUID> {
    List<Bandobast> findByStatus(OperationStatus status);
    List<Bandobast> findByKind(Bandobast.OperationKind kind);
}
