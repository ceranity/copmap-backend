package in.copmap.service;

import in.copmap.domain.Role;
import in.copmap.domain.User;
import in.copmap.exception.ApiException;
import in.copmap.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository users;
    private final PasswordEncoder encoder;

    public UserService(UserRepository users, PasswordEncoder encoder) {
        this.users = users; this.encoder = encoder;
    }

    @Transactional
    public User register(String username, String rawPassword, String fullName,
                         String badge, String phone, Role role) {
        if (users.existsByUsername(username)) throw ApiException.conflict("Username already taken");
        return users.save(User.builder()
                .username(username)
                .passwordHash(encoder.encode(rawPassword))
                .fullName(fullName)
                .badgeNumber(badge)
                .phone(phone)
                .role(role)
                .active(true)
                .build());
    }

    public User get(UUID id) { return users.findById(id).orElseThrow(() -> ApiException.notFound("User")); }
    public List<User> listOfficers() { return users.findByRole(Role.OFFICER); }
    public List<User> all() { return users.findAll(); }
}
