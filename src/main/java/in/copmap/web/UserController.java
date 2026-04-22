package in.copmap.web;

import in.copmap.domain.User;
import in.copmap.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import in.copmap.security.UserPrincipal;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService users;

    public UserController(UserService users) { this.users = users; }

    @GetMapping("/me")
    public User me(@AuthenticationPrincipal UserPrincipal principal) {
        User u = users.get(principal.id());
        u.setPasswordHash(null);
        return u;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PLANNER','SUPERVISOR')")
    public List<User> all() {
        List<User> all = users.all();
        all.forEach(u -> u.setPasswordHash(null));
        return all;
    }

    @GetMapping("/officers")
    @PreAuthorize("hasAnyRole('PLANNER','SUPERVISOR')")
    public List<User> officers() {
        List<User> officers = users.listOfficers();
        officers.forEach(u -> u.setPasswordHash(null));
        return officers;
    }
}
