package com.nousware.service;

import com.nousware.dto.RegistrationRequest;
import com.nousware.dto.UpdateUserRequest;        // DTO for partial updates
import com.nousware.entities.User;
import org.springframework.data.domain.Page;       // Spring Data page result
import org.springframework.data.domain.Pageable;  // Spring Data pagination/sorting
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface UserService {

    // Outcome for verify-or-resend flow
    enum VerifyResult { VERIFIED, RESENT_NEW_LINK, INVALID }

    // ===================== Registration / Verification =====================

    /** Register a new LOCAL user and send verification email. */
    @Transactional
    void registerUser(RegistrationRequest request);

    /** Verify account by token (may mutate user + delete token). */
    @Transactional
    boolean verifyAccount(String token);

    /**
     * Verify a token if valid; if expired but belongs to an unverified user,
     * delete it and send a fresh verification email.
     * @return VERIFIED | RESENT_NEW_LINK | INVALID
     */
    @Transactional
    VerifyResult verifyOrResend(String token);

    /** Resend verification email if user exists and is not enabled. */
    @Transactional
    void resendVerification(String email);

    // ===================== Forgot / Reset Password =====================

    /** Request a password reset (creates token and emails link). */
    @Transactional
    void requestPasswordReset(String email);

    /** Reset password using a valid token. */
    @Transactional
    void resetPassword(String token, String newPassword);


    // ===================== Lookups / Helpers =====================

    /** Load user by email or throw (used by /me and internal logic). */
    @Transactional(readOnly = true)
    User getByEmailOrThrow(String email);

    /** Optional, nullable-style lookup by email (case-insensitive). */
    @Transactional(readOnly = true)
    Optional<User> findByEmailIgnoreCase(String email);

    /** Mark last successful login time for LOCAL login. */
    @Transactional
    void markLoginSuccess(String email);


    // ===================== Google OAuth2 Support =====================

    /** Find a user by email (case-insensitive) or Google sub. */
    @Transactional(readOnly = true)
    Optional<User> findByEmailOrGoogleSub(String email, String googleSub);

    /** Create or update a user record from Google profile data. */
    @Transactional
    User upsertGoogleUser(String googleSub, String email, String name, String pictureUrl);


    // ===================== Avatar URL (user-uploaded) =====================

    /** Set/update user's uploaded avatar URL (pass null/"" to clear). */
    @Transactional
    User updateAvatar(Integer userId, String avatarUrl);

    /** Explicitly clear user's uploaded avatar. */
    @Transactional
    User clearAvatar(Integer userId);


    // ===================== User Management (List / Get / Edit) =====================

    /** Page through all users with sorting support via Pageable. */
    @Transactional(readOnly = true)
    Page<User> findAll(Pageable pageable);

    /** Find a single user by id (empty if not found). */
    @Transactional(readOnly = true)
    Optional<User> findById(Integer id);

    /**
     * Update selected fields of a user and persist.
     * Only non-null fields in UpdateUserRequest are applied.
     */
    @Transactional
    User updateUser(Integer userId, UpdateUserRequest request);

    /** Convenience save for internal use. */
    @Transactional
    User save(User user);
}
