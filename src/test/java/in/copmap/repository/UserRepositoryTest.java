package in.copmap.repository;

import in.copmap.domain.Role;
import in.copmap.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired UserRepository users;

    @Test
    void savesAndFindsByUsername() {
        users.save(User.builder()
                .username("officer01").passwordHash("h").fullName("R K")
                .role(Role.OFFICER).active(true).build());

        assertThat(users.existsByUsername("officer01")).isTrue();
        assertThat(users.findByUsername("officer01")).isPresent();
        assertThat(users.existsByUsername("nobody")).isFalse();
    }

    @Test
    void findByRoleFiltersCorrectly() {
        users.save(User.builder().username("o1").passwordHash("h").fullName("A").role(Role.OFFICER).active(true).build());
        users.save(User.builder().username("o2").passwordHash("h").fullName("B").role(Role.OFFICER).active(true).build());
        users.save(User.builder().username("p1").passwordHash("h").fullName("C").role(Role.PLANNER).active(true).build());

        List<User> officers = users.findByRole(Role.OFFICER);
        assertThat(officers).hasSize(2).extracting(User::getUsername)
                .containsExactlyInAnyOrder("o1", "o2");
    }
}
