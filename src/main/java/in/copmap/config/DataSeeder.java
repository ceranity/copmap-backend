package in.copmap.config;

import in.copmap.domain.Role;
import in.copmap.repository.UserRepository;
import in.copmap.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Idempotently creates default users on first boot so reviewers can log in immediately.
 * Uses the runtime BCrypt encoder so the stored hash always matches the declared password.
 *
 *   supervisor / copmap123
 *   planner    / copmap123
 *   officer1   / copmap123
 *   officer2   / copmap123
 */
@Component
@Slf4j
@Profile("!test")
public class DataSeeder implements CommandLineRunner {

    private final UserRepository users;
    private final UserService service;

    public DataSeeder(UserRepository users, UserService service) {
        this.users = users; this.service = service;
    }

    @Override
    public void run(String... args) {
        seed("supervisor", "Supervisor Singh", "S-001", Role.SUPERVISOR);
        seed("planner",    "Planner Rao",      "P-101", Role.PLANNER);
        seed("officer1",   "Constable Reddy",  "O-201", Role.OFFICER);
        seed("officer2",   "Constable Verma",  "O-202", Role.OFFICER);
    }

    private void seed(String username, String fullName, String badge, Role role) {
        if (users.existsByUsername(username)) return;
        service.register(username, "copmap123", fullName, badge, null, role);
        log.info("Seeded user: {} ({})", username, role);
    }
}
