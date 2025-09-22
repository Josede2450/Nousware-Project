package com.nousware.controller;

import com.nousware.dto.LoginRequest;
import com.nousware.dto.RegistrationRequest;
import com.nousware.entities.Role;
import com.nousware.entities.User;
import com.nousware.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;

    // Frontend base URL to redirect to after verification (set in application.yml)
    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    public AuthController(UserService userService, AuthenticationManager authenticationManager) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
    }

    // ---------- Registration / Verification ----------

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegistrationRequest request) {
        userService.registerUser(request);
        return ResponseEntity.ok(Map.of(
                "message", "Registration successful. Please check your email to verify your account."
        ));
    }

    @GetMapping("/verify")
    public ResponseEntity<Void> verify(@RequestParam String token) {
        boolean verified = userService.verifyAccount(token);

        String target = UriComponentsBuilder
                .fromHttpUrl(frontendUrl + "/login")   // 👈 was /auth/login
                .queryParam("verified", verified ? "true" : "false")
                .build(true)
                .toUriString();

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(target))
                .build();
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendVerification(@Valid @RequestBody ResendVerificationRequest req) {
        userService.resendVerification(req.email());
        return ResponseEntity.ok(Map.of("message", "If the account exists and is unverified, a new link was sent."));
    }

    // ---------- Local Login (creates session cookie) ----------

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request,
                                   HttpServletRequest httpReq,
                                   HttpServletResponse httpRes) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            SecurityContextHolder.getContext().setAuthentication(auth);
            httpReq.getSession(true);
            new org.springframework.security.web.context.HttpSessionSecurityContextRepository()
                    .saveContext(
                            org.springframework.security.core.context.SecurityContextHolder.getContext(),
                            httpReq,
                            httpRes
                    );

            userService.markLoginSuccess(request.getEmail());
            return ResponseEntity.noContent().build();

        } catch (DisabledException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Account is not verified"));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid credentials"));
        }
    }

    // ---------- Google OAuth ----------

    @GetMapping("/login/google")
    public void loginWithGoogle(HttpServletResponse response) throws IOException {
        response.sendRedirect("/oauth2/authorization/google");
    }

    // ---------- Current User ----------

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal Object principal) {
        if (principal == null || "anonymousUser".equals(principal)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("authenticated", false));
        }

        // --- Google (OAuth2) ---
        if (principal instanceof OAuth2User oAuth2User) {
            String email = oAuth2User.getAttribute("email");
            if (email == null || email.isBlank()) {
                email = null;
            }

            User u = (email != null && !email.isBlank())
                    ? userService.getByEmailOrThrow(email)
                    : null;

            String avatar  = (u != null) ? u.getAvatarUrl()  : null;
            String picture = (u != null) ? u.getPictureUrl() : oAuth2User.getAttribute("picture");

            String display = (avatar != null && !avatar.isBlank())
                    ? avatar
                    : ((picture != null && !picture.isBlank()) ? picture : null);

            // Prefer DB values if found; else split OAuth2 "name"
            String fullName = (u != null && u.getName() != null && !u.getName().isBlank())
                    ? (u.getName() + (u.getLastName() != null ? " " + u.getLastName() : ""))
                    : oAuth2User.getAttribute("name");

            String[] firstLast = splitFirstLast(fullName);
            String firstName = (u != null) ? u.getName()     : firstLast[0];
            String lastName  = (u != null) ? u.getLastName() : firstLast[1];

            // roles: prefer entity; else authorities
            List<String> roles = (u != null)
                    ? normalizeAndSortRoles(roleNames(u.getRoles()))
                    : normalizeAndSortRoles(authorityNames(oAuth2User.getAuthorities()));

            Map<String, Object> out = new LinkedHashMap<>();
            out.put("authenticated", true);
            out.put("provider", "google");

            if (u != null) {
                out.put("id", u.getUserId());
                out.put("email", u.getEmail());
                out.put("firstName", firstName);
                out.put("lastName", lastName);
            } else {
                out.put("firstName", firstName);
                if (lastName != null) out.put("lastName", lastName);
                if (email != null) out.put("email", email);
            }

            out.put("role", topRole(roles)); // ADMIN if present
            out.put("roles", roles);

            if (avatar  != null) out.put("avatar", avatar);
            if (picture != null) out.put("picture", picture);
            if (display != null) out.put("displayPicture", display);

            return ResponseEntity.ok(out);
        }

        // --- Local (username/password) ---
        if (principal instanceof UserDetails springUser) {
            User u = userService.getByEmailOrThrow(springUser.getUsername());
            String avatar  = u.getAvatarUrl();
            String picture = u.getPictureUrl();
            String display = (avatar != null && !avatar.isBlank()) ? avatar
                    : ((picture != null && !picture.isBlank()) ? picture : null);

            // roles: prefer entity; fallback to principal authorities
            List<String> roles = normalizeAndSortRoles(roleNames(u.getRoles()));
            if (roles.isEmpty()) {
                roles = normalizeAndSortRoles(authorityNames(springUser.getAuthorities()));
            }

            Map<String, Object> out = new LinkedHashMap<>();
            out.put("authenticated", true);
            out.put("provider", "local");
            out.put("id", u.getUserId());
            out.put("email", u.getEmail());
            out.put("firstName", u.getName()); // 'name' == first name
            if (u.getLastName() != null) {
                out.put("lastName", u.getLastName());
            }

            out.put("role", topRole(roles)); // ADMIN if present
            out.put("roles", roles);

            if (avatar  != null) out.put("avatar", avatar);
            if (picture != null) out.put("picture", picture);
            if (display != null) out.put("displayPicture", display);

            return ResponseEntity.ok(out);
        }

        // --- Fallback (rare) ---
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("authenticated", true);
        out.put("principal", principal.toString());
        return ResponseEntity.ok(out);
    }

    // kept for compatibility (unused by /me now)
    private String safeRoleName(List<Role> roles) {
        return (roles != null && !roles.isEmpty())
                ? roles.stream().findFirst().map(Role::getRoleName).orElse("CLIENT")
                : "CLIENT";
    }

    // ---------- Avatar (current user) ----------

    @PatchMapping("/me/avatar")
    public ResponseEntity<?> updateMyAvatar(@AuthenticationPrincipal Object principal,
                                            @RequestBody AvatarUpdateRequest req) {
        if (principal == null || "anonymousUser".equals(principal)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("authenticated", false));
        }

        String email = null;
        if (principal instanceof OAuth2User oAuth2User) {
            email = oAuth2User.getAttribute("email");
        } else if (principal instanceof UserDetails springUser) {
            email = springUser.getUsername();
        }
        if (email == null || email.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unable to resolve user"));
        }

        User u = userService.getByEmailOrThrow(email);
        User updated = userService.updateAvatar(u.getUserId(), req.avatarUrl());

        String avatar  = updated.getAvatarUrl();
        String picture = updated.getPictureUrl();
        String display = (avatar != null && !avatar.isBlank()) ? avatar
                : (picture != null && !picture.isBlank() ? picture : null);

        return ResponseEntity.ok(Map.of(
                "message", (req.avatarUrl() == null || req.avatarUrl().isBlank()) ? "Avatar cleared" : "Avatar updated",
                "avatar", avatar,
                "picture", picture,
                "displayPicture", display
        ));
    }

    // ---------- Forgot / Reset Password ----------

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        userService.requestPasswordReset(req.email());
        return ResponseEntity.ok(Map.of("message", "If the account exists, a reset link has been sent."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        userService.resetPassword(req.token(), req.newPassword());
        return ResponseEntity.ok(Map.of("message", "Password reset successful."));
    }

    // ---------- Logout (invalidates session) ----------

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        new SecurityContextLogoutHandler().logout(
                request, response, SecurityContextHolder.getContext().getAuthentication()
        );
        return ResponseEntity.noContent().build();
    }

    // ---------- Request DTOs (compact, validated) ----------

    public record ResendVerificationRequest(@NotBlank @Email String email) {}
    public record ForgotPasswordRequest(@NotBlank @Email String email) {}
    public record ResetPasswordRequest(
            @NotBlank String token,
            @NotBlank @Size(min = 8, message = "Password must be at least 8 characters") String newPassword
    ) {}
    public record AvatarUpdateRequest(String avatarUrl) {}

    // ---------- Helpers ----------

    /** Split a full name into [first, last or null] without throwing. */
    private String[] splitFirstLast(String fullName) {
        if (fullName == null) return new String[]{null, null};
        String trimmed = fullName.trim();
        if (trimmed.isEmpty()) return new String[]{null, null};
        String[] parts = trimmed.split("\\s+");
        String first = parts[0];
        String last  = (parts.length > 1) ? parts[parts.length - 1] : null;
        return new String[]{first, last};
    }

    /** Convert entity roles to raw role-name list (may include ROLE_ prefix in some setups). */
    private List<String> roleNames(List<Role> roles) {
        if (roles == null) return List.of();
        return roles.stream()
                .filter(r -> r != null && r.getRoleName() != null)
                .map(Role::getRoleName)
                .collect(Collectors.toList());
    }

    /** Convert authorities to raw role-name list. */
    private List<String> authorityNames(Collection<? extends GrantedAuthority> authorities) {
        if (authorities == null) return List.of();
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
    }

    /** Normalize a single role: strip ROLE_ prefix, uppercase. */
    private String normalizeRole(String role) {
        if (role == null) return null;
        String s = role.trim().toUpperCase();
        if (s.startsWith("ROLE_")) s = s.substring(5);
        return s;
    }

    /** Normalize and sort roles with ADMIN first, then alphabetically. */
    private List<String> normalizeAndSortRoles(List<String> raw) {
        return raw.stream()
                .map(this::normalizeRole)
                .filter(s -> s != null && !s.isBlank())
                .distinct()
                .sorted(Comparator.comparing((String r) -> !r.equals("ADMIN")) // ADMIN -> false -> first
                        .thenComparing(Comparator.naturalOrder()))
                .collect(Collectors.toList());
    }

    /** Choose top role: ADMIN if present; else first; else CLIENT. */
    private String topRole(List<String> roles) {
        if (roles == null || roles.isEmpty()) return "CLIENT";
        if (roles.stream().anyMatch(r -> "ADMIN".equalsIgnoreCase(r))) return "ADMIN";
        return roles.get(0);
    }
}
