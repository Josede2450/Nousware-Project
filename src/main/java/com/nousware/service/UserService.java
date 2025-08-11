package com.nousware.service;

import com.nousware.dto.RegistrationRequest;
import com.nousware.entities.User;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface UserService {
    void registerUser(RegistrationRequest request);
    boolean verifyAccount(String token);

    @Transactional
    void resendVerification(String email);

    // ---- Google OAuth2 support ----

    /** Find a user by email (case-insensitive) or Google sub. */
    @Transactional(readOnly = true)
    Optional<User> findByEmailOrGoogleSub(String email, String googleSub);

    /** Create or update a user record from Google profile data. */
    @Transactional
    User upsertGoogleUser(String googleSub, String email, String name, String pictureUrl);
}
