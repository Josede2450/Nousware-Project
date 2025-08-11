package com.nousware.service;

import com.nousware.dto.RegistrationRequest;
import com.nousware.entities.Role;
import com.nousware.entities.User;
import com.nousware.entities.VerificationToken;
import com.nousware.enums.TokenType; // <- EMAIL_VERIFY, PASSWORD_RESET
import com.nousware.repository.RoleRepository;
import com.nousware.repository.UserRepository;
import com.nousware.repository.VerificationTokenRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final VerificationTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final RoleRepository roleRepository;

    // TTL for tokens (minutes)
    private static final long TOKEN_TTL_MINUTES = 15;

    public UserServiceImpl(UserRepository userRepository,
                           VerificationTokenRepository tokenRepository,
                           PasswordEncoder passwordEncoder,
                           EmailService emailService,
                           RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.roleRepository = roleRepository;
    }

    // ===================== Local Registration / Verification =====================

    @Transactional
    @Override
    public void registerUser(RegistrationRequest request) {
        // Normalize email
        String normalizedEmail = normalizeEmail(request.getEmail());

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(normalizedEmail);
        user.setGender(request.getGender());
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEnable(false);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        Role clientRole = roleRepository.findByRoleName("CLIENT")
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR, "Client role not found"));
        user.setRoles(List.of(clientRole));

        userRepository.save(user);

        String token = UUID.randomUUID().toString();
        VerificationToken vt = new VerificationToken();
        vt.setToken(token);
        vt.setUser(user);
        vt.setExpiryDate(LocalDateTime.now().plusMinutes(TOKEN_TTL_MINUTES));
        vt.setTokenType(TokenType.EMAIL_VERIFY);
        tokenRepository.save(vt);

        emailService.sendVerificationEmail(user.getEmail(), token);
    }

    @Transactional
    @Override
    public boolean verifyAccount(String token) {
        return tokenRepository.findByToken(token)
                .filter(t -> t.getTokenType() == TokenType.EMAIL_VERIFY)
                .filter(t -> t.getExpiryDate().isAfter(LocalDateTime.now()))
                .map(t -> {
                    User user = t.getUser();
                    if (!user.isEnable()) {
                        user.setEnable(true);
                        user.setUpdatedAt(LocalDateTime.now());
                        userRepository.save(user);
                    }
                    tokenRepository.delete(t);
                    return true;
                })
                .orElse(false);
    }

    @Transactional
    @Override
    public void resendVerification(String email) {
        String normalizedEmail = normalizeEmail(email);
        userRepository.findByEmailIgnoreCase(normalizedEmail).ifPresent(user -> {
            if (user.isEnable()) return;

            List<VerificationToken> old = tokenRepository.findByUser_UserId(user.getUserId());
            old.stream()
                    .filter(v -> v.getTokenType() == TokenType.EMAIL_VERIFY)
                    .forEach(tokenRepository::delete);

            String token = UUID.randomUUID().toString();
            VerificationToken vt = new VerificationToken();
            vt.setToken(token);
            vt.setUser(user);
            vt.setExpiryDate(LocalDateTime.now().plusMinutes(TOKEN_TTL_MINUTES));
            vt.setTokenType(TokenType.EMAIL_VERIFY);
            tokenRepository.save(vt);

            emailService.sendVerificationEmail(user.getEmail(), token);
        });
    }

    // ===================== Forgot / Reset Password =====================

    @Transactional
    @Override
    public void requestPasswordReset(String email) {
        String normalizedEmail = normalizeEmail(email);
        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(normalizedEmail);
        if (userOpt.isEmpty()) {
            return; // No leak
        }

        User user = userOpt.get();

        List<VerificationToken> old = tokenRepository.findByUser_UserId(user.getUserId());
        old.stream()
                .filter(v -> v.getTokenType() == TokenType.PASSWORD_RESET)
                .forEach(tokenRepository::delete);

        String token = UUID.randomUUID().toString();
        VerificationToken vt = new VerificationToken();
        vt.setToken(token);
        vt.setUser(user);
        vt.setExpiryDate(LocalDateTime.now().plusMinutes(TOKEN_TTL_MINUTES));
        vt.setTokenType(TokenType.PASSWORD_RESET);
        tokenRepository.save(vt);

        emailService.sendPasswordResetEmail(user.getEmail(), token);
    }

    @Transactional
    @Override
    public void resetPassword(String token, String newPassword) {
        VerificationToken vt = tokenRepository.findByToken(token)
                .filter(t -> t.getTokenType() == TokenType.PASSWORD_RESET)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired token"));

        if (vt.getExpiryDate() == null || vt.getExpiryDate().isBefore(LocalDateTime.now())) {
            tokenRepository.delete(vt);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired token");
        }

        User user = vt.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        tokenRepository.delete(vt);
    }

    // ===================== Google OAuth2 Support =====================

    @Transactional(readOnly = true)
    @Override
    public Optional<User> findByEmailOrGoogleSub(String email, String googleSub) {
        String normalizedEmail = normalizeEmail(email);
        return userRepository.findByEmailIgnoreCaseOrGoogleSub(normalizedEmail, googleSub);
    }

    @Transactional
    @Override
    public User upsertGoogleUser(String googleSub, String email, String name, String pictureUrl) {
        String normalizedEmail = normalizeEmail(email);

        User user = userRepository.findByGoogleSub(googleSub)
                .orElseGet(() -> userRepository.findByEmailIgnoreCase(normalizedEmail).orElse(null));

        if (user == null) {
            user = new User();
            user.setEmail(normalizedEmail);
            user.setName(name);
            user.setPictureUrl(pictureUrl);
            user.setGoogleSub(googleSub);
            user.setProvider("GOOGLE");
            user.setEnable(true);
            user.setPassword(null);
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());
            user.setLastLoginAt(LocalDateTime.now());

            Role clientRole = roleRepository.findByRoleName("CLIENT")
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.INTERNAL_SERVER_ERROR, "Client role not found"));
            user.setRoles(List.of(clientRole));

            return userRepository.save(user);
        }

        if (user.getGoogleSub() == null) {
            user.setGoogleSub(googleSub);
            user.setProvider("GOOGLE");
        }
        if (name != null && !name.isBlank()) user.setName(name);
        if (pictureUrl != null && !pictureUrl.isBlank()) user.setPictureUrl(pictureUrl);
        if (!user.isEnable()) user.setEnable(true);

        user.setLastLoginAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    // ===================== Helper =====================

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
