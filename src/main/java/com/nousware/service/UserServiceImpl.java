package com.nousware.service;

import com.nousware.dto.RegistrationRequest;
import com.nousware.dto.UpdateUserRequest;            // <-- NEW: DTO for partial updates
import com.nousware.entities.Role;
import com.nousware.entities.User;
import com.nousware.entities.VerificationToken;
import com.nousware.enums.TokenType;
import com.nousware.repository.RoleRepository;
import com.nousware.repository.UserRepository;
import com.nousware.repository.VerificationTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;             // <-- NEW: Page result
import org.springframework.data.domain.Pageable;       // <-- NEW: Pagination + sorting
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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

    /** Default fallback profile picture (used when avatarUrl and pictureUrl are both missing). */
    private static final String DEFAULT_PFP =
            "https://i.pinimg.com/736x/27/5f/99/275f99923b080b18e7b474ed6155a17f.jpg";

    /** Token TTL (minutes), configurable via application.properties: app.auth.token-ttl-minutes */
    @Value("${app.auth.token-ttl-minutes:15}")
    private long tokenTtlMinutes;

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
        String normalizedEmail = normalizeEmail(request.getEmail());

        // Case-insensitive existence check
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
        }

        // Derive names safely
        String firstName = null;
        String lastName  = null;

        // If DTO has lastName field, use it; otherwise split the provided name
        try {
            var m = request.getClass().getMethod("getLastName");
            Object ln = m.invoke(request);
            if (ln instanceof String s && !s.isBlank()) {
                lastName = s.trim();
            }
        } catch (Exception ignore) {}

        if (lastName == null) {
            // Try to split request.getName() into first/last
            String[] parts = splitFirstLast(request.getName());
            firstName = parts[0];
            lastName  = parts[1];
        } else {
            // If lastName provided, treat request.getName() as first name (or full name → take first token)
            String[] parts = splitFirstLast(request.getName());
            firstName = parts[0];
        }

        User user = new User();
        user.setName(firstName);            // first name stored in 'name'
        user.setLastName(lastName);
        user.setEmail(normalizedEmail);
        user.setGender(request.getGender());
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEnable(false);              // requires email verification
        user.setProvider("LOCAL");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        Role clientRole = roleRepository.findByRoleName("CLIENT")
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR, "Client role not found"));

        // ---- assign a MUTABLE roles list
        user.setRoles(new java.util.ArrayList<>(java.util.List.of(clientRole)));

        userRepository.save(user);

        // create verification token
        VerificationToken vt = new VerificationToken();
        vt.setToken(UUID.randomUUID().toString());
        vt.setUser(user);
        vt.setExpiryDate(LocalDateTime.now().plusMinutes(tokenTtlMinutes));
        vt.setTokenType(TokenType.EMAIL_VERIFY);
        tokenRepository.save(vt);

        emailService.sendVerificationEmail(user.getEmail(), vt.getToken());
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

    /**
     * NEW: verify-or-resend behavior.
     * If token is valid → enable and delete it.
     * If token is expired → delete it and send a fresh verification link.
     * If token not found / wrong type → INVALID.
     */
    @Transactional
    @Override
    public VerifyResult verifyOrResend(String token) {
        Optional<VerificationToken> opt = tokenRepository.findByToken(token);
        if (opt.isEmpty()) return VerifyResult.INVALID;

        VerificationToken vt = opt.get();
        if (vt.getTokenType() != TokenType.EMAIL_VERIFY) {
            return VerifyResult.INVALID;
        }

        if (vt.getExpiryDate() != null && vt.getExpiryDate().isAfter(LocalDateTime.now())) {
            // valid → verify
            User user = vt.getUser();
            if (!user.isEnable()) {
                user.setEnable(true);
                user.setUpdatedAt(LocalDateTime.now());
                userRepository.save(user);
            }
            tokenRepository.delete(vt);
            return VerifyResult.VERIFIED;
        }

        // expired → delete old verify tokens and send a brand-new one
        User user = vt.getUser();
        // remove any existing EMAIL_VERIFY tokens for this user
        tokenRepository.findByUser_UserId(user.getUserId()).stream()
                .filter(v -> v.getTokenType() == TokenType.EMAIL_VERIFY)
                .forEach(tokenRepository::delete);

        VerificationToken fresh = new VerificationToken();
        fresh.setToken(UUID.randomUUID().toString());
        fresh.setUser(user);
        fresh.setExpiryDate(LocalDateTime.now().plusMinutes(tokenTtlMinutes));
        fresh.setTokenType(TokenType.EMAIL_VERIFY);
        tokenRepository.save(fresh);

        emailService.sendVerificationEmail(user.getEmail(), fresh.getToken());
        return VerifyResult.RESENT_NEW_LINK;
    }

    @Transactional
    @Override
    public void resendVerification(String email) {
        String normalizedEmail = normalizeEmail(email);
        userRepository.findByEmailIgnoreCase(normalizedEmail).ifPresent(user -> {
            if (user.isEnable()) return;

            tokenRepository.deleteByUserIdAndType(user.getUserId(), TokenType.EMAIL_VERIFY);
            tokenRepository.flush();

            var vt = new VerificationToken();
            vt.setToken(java.util.UUID.randomUUID().toString());
            vt.setUser(user);
            vt.setExpiryDate(java.time.LocalDateTime.now().plusMinutes(tokenTtlMinutes));
            vt.setTokenType(TokenType.EMAIL_VERIFY);

            tokenRepository.saveAndFlush(vt);

            emailService.sendVerificationEmail(user.getEmail(), vt.getToken());
        });
    }

    // ===================== Forgot / Reset Password =====================

    @Transactional
    @Override
    public void requestPasswordReset(String email) {
        String normalizedEmail = normalizeEmail(email);
        var userOpt = userRepository.findByEmailIgnoreCase(normalizedEmail);
        if (userOpt.isEmpty()) return;

        var user = userOpt.get();

        // 1) hard-delete any prior PASSWORD_RESET tokens for this user
        tokenRepository.deleteByUserIdAndType(user.getUserId(), TokenType.PASSWORD_RESET);

        // 2) ensure the delete is flushed so the unique index won't trip on insert
        tokenRepository.flush();

        // 3) create fresh token
        var vt = new VerificationToken();
        vt.setToken(java.util.UUID.randomUUID().toString());
        vt.setUser(user);
        vt.setExpiryDate(java.time.LocalDateTime.now().plusMinutes(tokenTtlMinutes));
        vt.setTokenType(TokenType.PASSWORD_RESET);

        tokenRepository.saveAndFlush(vt); // save + flush to be safe

        emailService.sendPasswordResetEmail(user.getEmail(), vt.getToken());
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

        String[] parts = splitFirstLast(name);
        String first = parts[0];
        String last  = parts[1];

        if (user == null) {
            user = new User();
            user.setEmail(normalizedEmail);
            user.setName(first);
            user.setLastName(last);
            user.setPictureUrl(pictureUrl); // OAuth-provided picture (not our avatar)
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

            // assign a MUTABLE roles list
            user.setRoles(new java.util.ArrayList<>(java.util.List.of(clientRole)));

            return userRepository.save(user);
        }

        if (user.getGoogleSub() == null) {
            user.setGoogleSub(googleSub);
            user.setProvider("GOOGLE");
        }
        if (first != null && !first.isBlank()) user.setName(first);
        if (last != null && !last.isBlank())   user.setLastName(last);
        if (pictureUrl != null && !pictureUrl.isBlank()) user.setPictureUrl(pictureUrl);
        if (!user.isEnable()) user.setEnable(true);

        user.setLastLoginAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    // ===================== Avatar URL (user-uploaded) =====================

    /** Sets/updates the user's uploaded avatar URL (separate from OAuth pictureUrl). Accepts null/blank to clear the avatar. */
    @Transactional
    @Override
    public User updateAvatar(Integer userId, String avatarUrl) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        String cleaned = sanitizeUrl(avatarUrl); // null if blank
        user.setAvatarUrl(cleaned);
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    /** Convenience method to clear the user's uploaded avatar. */
    @Transactional
    @Override
    public User clearAvatar(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        user.setAvatarUrl(null);
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    // ===================== Lookups / Helpers =====================

    @Transactional(readOnly = true)
    @Override
    public User getByEmailOrThrow(String email) {
        String normalized = normalizeEmail(email);
        return userRepository.findByEmailIgnoreCase(normalized)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + normalized));
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<User> findByEmailIgnoreCase(String email) {
        return userRepository.findByEmailIgnoreCase(normalizeEmail(email));
    }

    @Transactional
    @Override
    public void markLoginSuccess(String email) {
        String normalized = normalizeEmail(email);
        userRepository.findByEmailIgnoreCase(normalized).ifPresent(u -> {
            u.setLastLoginAt(LocalDateTime.now());
            u.setUpdatedAt(LocalDateTime.now());
            userRepository.save(u);
        });
    }

    /** Prefer avatarUrl (uploaded) if present; else fallback to OAuth picture; else default image. */
    public String getDisplayPictureUrl(User u) {
        if (u == null) return DEFAULT_PFP;
        if (u.getAvatarUrl() != null && !u.getAvatarUrl().isBlank()) return u.getAvatarUrl();
        if (u.getPictureUrl() != null && !u.getPictureUrl().isBlank()) return u.getPictureUrl();
        return DEFAULT_PFP;
    }

    // ===================== NEW: User Management (List / Get / Edit) =====================

    /** Page through all users with sorting. */
    @Transactional(readOnly = true)
    @Override
    public Page<User> findAll(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    /** Find a single user by id (empty if not found). */
    @Transactional(readOnly = true)
    @Override
    public Optional<User> findById(Integer id) {
        return userRepository.findById(id);
    }

    /**
     * Partially update a user.
     * Rules:
     *  - Only non-null fields in the request are applied.
     *  - Email is normalized and uniqueness-checked.
     *  - roles == null -> unchanged; roles == [] -> clear by mutating; else replace via mutating the managed collection.
     *  - avatarUrl: null -> unchanged; "" -> clear; non-empty -> sanitized & set.
     */
    @Transactional
    @Override
    public User updateUser(Integer userId, UpdateUserRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (req.email != null) {
            String newEmail = normalizeEmail(req.email);
            if (!newEmail.equalsIgnoreCase(user.getEmail())
                    && userRepository.existsByEmailIgnoreCase(newEmail)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
            }
            user.setEmail(newEmail);
        }

        if (req.firstName != null) {
            String v = req.firstName.trim();
            user.setName(v.isEmpty() ? null : v);
        }
        if (req.lastName != null) {
            String v = req.lastName.trim();
            user.setLastName(v.isEmpty() ? null : v);
        }

        if (req.phone != null) {
            String v = req.phone.trim();
            user.setPhone(v.isEmpty() ? null : v);
        }
        if (req.gender != null) {
            String v = req.gender.trim();
            user.setGender(v.isEmpty() ? null : v);
        }

        if (req.enabled != null) {
            user.setEnable(req.enabled);
        }

        if (req.avatarUrl != null) {
            String raw = req.avatarUrl.trim();
            String cleaned = raw.isEmpty() ? null : sanitizeUrl(raw);
            user.setAvatarUrl(cleaned);
        }

        if (req.roles != null) {
            if (user.getRoles() == null) {
                user.setRoles(new java.util.ArrayList<>()); // ensure mutable
            }
            if (req.roles.isEmpty()) {
                user.getRoles().clear();
            } else {
                java.util.ArrayList<Role> newRoles = req.roles.stream()
                        .map(name -> roleRepository.findByRoleName(name)
                                .orElseThrow(() -> new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST, "Unknown role: " + name)))
                        .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));

                user.getRoles().clear();
                user.getRoles().addAll(newRoles);
            }
        }

        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    // ===================== Internal helpers =====================

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    /** Accepts only http/https URLs; returns null on null/blank input. Throws 400 if invalid. */
    private String sanitizeUrl(String url) {
        if (url == null) return null;
        String t = url.trim();
        if (t.isEmpty()) return null;
        if (!(t.startsWith("http://") || t.startsWith("https://"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid avatar URL");
        }
        if (t.length() > 1024) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "URL too long");
        }
        return t;
    }

    /** Split a full name into [first, last or null]. */
    private String[] splitFirstLast(String fullName) {
        if (fullName == null) return new String[]{null, null};
        String trimmed = fullName.trim();
        if (trimmed.isEmpty()) return new String[]{null, null};
        String[] parts = trimmed.split("\\s+");
        String first = parts[0];
        String last  = (parts.length > 1) ? parts[parts.length - 1] : null;
        return new String[]{first, last};
    }

    @Transactional
    @Override
    public User save(User user) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is null");
        }
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }
}
