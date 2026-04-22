package in.copmap.web;

import in.copmap.audit.AuditService;
import in.copmap.domain.User;
import in.copmap.security.JwtService;
import in.copmap.security.UserPrincipal;
import in.copmap.service.UserService;
import in.copmap.web.dto.AuthDtos.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authManager;
    private final JwtService jwt;
    private final UserService userService;
    private final AuditService audit;

    public AuthController(AuthenticationManager authManager, JwtService jwt,
                          UserService userService, AuditService audit) {
        this.authManager = authManager; this.jwt = jwt;
        this.userService = userService; this.audit = audit;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        var auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.username(), req.password()));
        UserPrincipal p = (UserPrincipal) auth.getPrincipal();
        User u = userService.get(p.id());
        audit.record("LOGIN", "User", p.id(), null);
        return new LoginResponse(jwt.issue(p), u.getUsername(), u.getRole(), u.getFullName());
    }

    @PostMapping("/register")
    public ResponseEntity<User> register(@Valid @RequestBody RegisterRequest req) {
        User created = userService.register(req.username(), req.password(), req.fullName(),
                req.badgeNumber(), req.phone(), req.role());
        audit.record("REGISTER", "User", created.getId(), "role=" + created.getRole());
        created.setPasswordHash(null);
        return ResponseEntity.ok(created);
    }
}
