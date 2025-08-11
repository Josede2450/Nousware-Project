package com.nousware.controller;

import com.nousware.dto.LoginRequest;
import com.nousware.dto.RegistrationRequest;
import com.nousware.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
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

    // --- EXISTING ENDPOINTS ---

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegistrationRequest request) {
        userService.registerUser(request);
        return ResponseEntity.ok("Registration successful. Please check your email to verify your account.");
    }

    @GetMapping("/verify")
    public ResponseEntity<String> verify(@RequestParam String token) {
        boolean verified = userService.verifyAccount(token);
        if (verified) {
            return ResponseEntity.ok("Account verified successfully. You can now log in.");
        }
        return ResponseEntity.badRequest().body("Invalid or expired token.");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
            return ResponseEntity.ok("Login successful");
        } catch (DisabledException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Account is not verified");
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
        }
    }

    // --- NEW GOOGLE OAUTH ENDPOINTS ---

    /**
     * Start Google OAuth2 login by redirecting to Spring Security's /oauth2/authorization/google.
     * This will send the user to Google's login page.
     */
    @GetMapping("/login/google")
    public void loginWithGoogle(HttpServletResponse response) throws IOException {
        response.sendRedirect("/oauth2/authorization/google");
    }

    /**
     * Returns info about the currently logged-in user (works for Google or local login).
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal Object principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("authenticated", false));
        }

        // If logged in via Google OAuth
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

        // If logged in via normal username/password
        if (principal instanceof org.springframework.security.core.userdetails.User user) {
            return ResponseEntity.ok(Map.of(
                    "authenticated", true,
                    "provider", "local",
                    "username", user.getUsername()
            ));
        }

        return ResponseEntity.ok(Map.of("authenticated", true, "principal", principal.toString()));
    }
}
