package in.copmap.web.dto;

import in.copmap.domain.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthDtos {
    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
    public record LoginResponse(String token, String username, Role role, String fullName) {}
    public record RegisterRequest(
            @NotBlank @Size(min = 3, max = 64) String username,
            @NotBlank @Size(min = 6, max = 64) String password,
            @NotBlank String fullName,
            String badgeNumber,
            String phone,
            Role role) {}
}
