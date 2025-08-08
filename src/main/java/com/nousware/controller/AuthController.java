package com.nousware.controller;

import com.nousware.dto.RegistrationRequest;
import com.nousware.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

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
}
