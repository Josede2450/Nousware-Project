package com.nousware.controller;

import com.nousware.dto.LoginRequest;
import com.nousware.dto.RegistrationRequest;
import com.nousware.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;

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
    public ResponseEntity<?> verify(@RequestParam String token) {
        boolean verified = userService.verifyAccount(token);
        if (verified) return ResponseEntity.ok(Map.of("message", "Account verified successfully. You can now log in."));
        return ResponseEntity.badRequest().body(Map.of("error", "Invalid or expired token."));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendVerification(@Valid @RequestBody ResendVerificationRequest req) {
        userService.resendVerification(req.email());
        // Always return OK (don’t leak existence)
        return ResponseEntity.ok(Map.of("message", "If the account exists and is unverified, a new link was sent."));
    }

    // ---------- Local Login ----------

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
            return ResponseEntity.ok(Map.of("message", "Login successful"));
        } catch (DisabledException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Account is not verified"));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid credentials"));
        }
    }

    // ---------- Google OAuth ----------

    @GetMapping("/login/google")
    public void loginWithGoogle(HttpServletResponse response) throws IOException {
        response.sendRedirect("/oauth2/authorization/google");
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal Object principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("authenticated", false));
        }
        if (principal instanceof OAuth2User oAuth2User) {
            return ResponseEntity.ok(Map.of(
                    "authenticated", true,
                    "provider", "google",
                    "name", oAuth2User.getAttribute("name"),
                    "email", oAuth2User.getAttribute("email"),
                    "picture", oAuth2User.getAttribute("picture"),
                    "sub", oAuth2User.getAttribute("sub")
            ));
        }
        if (principal instanceof org.springframework.security.core.userdetails.User user) {
            return ResponseEntity.ok(Map.of(
                    "authenticated", true,
                    "provider", "local",
                    "username", user.getUsername()
            ));
        }
        return ResponseEntity.ok(Map.of("authenticated", true, "principal", principal.toString()));
    }

    // ---------- Forgot / Reset Password ----------

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        userService.requestPasswordReset(req.email());
        // Always 200 to avoid user enumeration
        return ResponseEntity.ok(Map.of("message", "If the account exists, a reset link has been sent."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        userService.resetPassword(req.token(), req.newPassword());
        return ResponseEntity.ok(Map.of("message", "Password reset successful."));
    }

    // ---------- Request DTOs (compact, validated) ----------

    public record ResendVerificationRequest(
            @NotBlank @Email String email
    ) {}

    public record ForgotPasswordRequest(
            @NotBlank @Email String email
    ) {}

    public record ResetPasswordRequest(
            @NotBlank String token,
            @NotBlank @Size(min = 8, message = "Password must be at least 8 characters")
            String newPassword
    ) {}
}
