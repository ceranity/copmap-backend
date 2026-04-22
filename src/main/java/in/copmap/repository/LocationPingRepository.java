package in.copmap.repository;

import in.copmap.domain.LocationPing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface LocationPingRepository extends JpaRepository<LocationPing, UUID> {

    @Query("SELECT p FROM LocationPing p WHERE p.officerId = :officerId " +
           "AND p.recordedAt BETWEEN :from AND :to ORDER BY p.recordedAt ASC")
    List<LocationPing> history(@Param("officerId") UUID officerId,
                               @Param("from") Instant from,
                               @Param("to") Instant to);
}
