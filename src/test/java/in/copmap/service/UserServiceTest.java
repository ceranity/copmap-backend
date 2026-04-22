package in.copmap.service;

import in.copmap.domain.Role;
import in.copmap.domain.User;
import in.copmap.exception.ApiException;
import in.copmap.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserServiceTest {

    UserRepository users;
    PasswordEncoder encoder;
    UserService service;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        encoder = mock(PasswordEncoder.class);
        service = new UserService(users, encoder);
    }

    @Test
    void registerHashesPasswordAndSaves() {
        when(users.existsByUsername("rk01")).thenReturn(false);
        when(encoder.encode("secret")).thenReturn("HASHED");
        when(users.save(any(User.class))).thenAnswer(i -> {
            User u = i.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });

        User u = service.register("rk01", "secret", "R Kumar", "B-101", "9999999999", Role.OFFICER);

        assertThat(u.getId()).isNotNull();
        assertThat(u.getPasswordHash()).isEqualTo("HASHED");
        assertThat(u.getRole()).isEqualTo(Role.OFFICER);
        assertThat(u.isActive()).isTrue();
        verify(encoder).encode("secret");
    }

    @Test
    void registerRejectsDuplicateUsername() {
        when(users.existsByUsername("rk01")).thenReturn(true);
        assertThatThrownBy(() -> service.register("rk01", "s", "R", "B", "9", Role.OFFICER))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("taken");
        verify(users, never()).save(any());
    }
}
