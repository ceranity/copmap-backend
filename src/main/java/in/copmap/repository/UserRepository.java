package in.copmap.repository;

import in.copmap.domain.Role;
import in.copmap.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    List<User> findByRole(Role role);
    List<User> findByIdIn(List<UUID> ids);
}
