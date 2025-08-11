package com.nousware.service;

import com.nousware.dto.RegistrationRequest;
import com.nousware.entities.Role;
import com.nousware.entities.User;
import com.nousware.entities.VerificationToken;
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
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setGender(request.getGender());
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEnable(false); // will be enabled after email verification
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        Role clientRole = roleRepository.findByRoleName("CLIENT")
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR, "Client role not found"));
        user.setRoles(List.of(clientRole));

        userRepository.save(user);

        String token = UUID.randomUUID().toString();
        VerificationToken verificationToken = new VerificationToken();
        verificationToken.setToken(token);
        verificationToken.setUser(user);
        verificationToken.setExpiryDate(LocalDateTime.now().plusMinutes(15));
        tokenRepository.save(verificationToken);

        emailService.sendVerificationEmail(user.getEmail(), token);
    }

    @Transactional
    @Override
    public boolean verifyAccount(String token) {
        return tokenRepository.findByToken(token)
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
        userRepository.findByEmailIgnoreCase(email).ifPresent(user -> {
            if (user.isEnable()) return;

            List<VerificationToken> old = tokenRepository.findByUser_UserId(user.getUserId());
            old.forEach(tokenRepository::delete);

            String token = UUID.randomUUID().toString();
            VerificationToken vt = new VerificationToken();
            vt.setToken(token);
            vt.setUser(user);
            vt.setExpiryDate(LocalDateTime.now().plusMinutes(15));
            tokenRepository.save(vt);

            emailService.sendVerificationEmail(user.getEmail(), token);
        });
    }

    // ===================== Google OAuth2 Support =====================

    /**
     * Find a user by email (case-insensitive) OR by Google subject (sub).
     * Useful to avoid duplicates when a previously registered user logs in with Google.
     */
    @Transactional(readOnly = true)
    @Override
    public Optional<User> findByEmailOrGoogleSub(String email, String googleSub) {
        return userRepository.findByEmailIgnoreCaseOrGoogleSub(email, googleSub);
    }

    /**
     * Create or update a user from Google profile data.
     * - If user exists by googleSub or email → update profile + link googleSub.
     * - If new → create a verified CLIENT user with provider=GOOGLE and no password.
     */
    @Transactional
    @Override
    public User upsertGoogleUser(String googleSub, String email, String name, String pictureUrl) {
        // Prefer lookup by googleSub, fallback to email
        User user = userRepository.findByGoogleSub(googleSub)
                .orElseGet(() -> userRepository.findByEmailIgnoreCase(email).orElse(null));

        if (user == null) {
            // Create new Google user
            user = new User();
            user.setEmail(email);
            user.setName(name);
            user.setPictureUrl(pictureUrl);
            user.setGoogleSub(googleSub);
            user.setProvider("GOOGLE"); // or enum
            user.setEnable(true);       // Google gives us a verified email; mark enabled
            user.setPassword(null);     // No local password for Google-auth accounts
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());
            user.setLastLoginAt(LocalDateTime.now());

            Role clientRole = roleRepository.findByRoleName("CLIENT")
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.INTERNAL_SERVER_ERROR, "Client role not found"));
            user.setRoles(List.of(clientRole));

            return userRepository.save(user);
        }

        // Update existing user
        // Link Google if not linked yet
        if (user.getGoogleSub() == null) {
            user.setGoogleSub(googleSub);
            user.setProvider("GOOGLE");
        }
        // Refresh profile data (keep it non-destructive)
        if (name != null && !name.isBlank()) user.setName(name);
        if (pictureUrl != null && !pictureUrl.isBlank()) user.setPictureUrl(pictureUrl);

        // Consider enabling if previously unverified local account and same email
        if (!user.isEnable()) {
            user.setEnable(true);
        }

        user.setLastLoginAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }
}
