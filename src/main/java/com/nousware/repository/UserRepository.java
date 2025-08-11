package com.nousware.repository;

import com.nousware.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository for managing {@link User} entities.
 */
public interface UserRepository extends JpaRepository<User, Integer> {

    /** Check if a user exists by email (case-sensitive in DB collation). */
    boolean existsByEmail(String email);

    /** Find by email (legacy use). Prefer the IgnoreCase variant. */
    Optional<User> findByEmail(String email);

    /** Find by email (case-insensitive). */
    Optional<User> findByEmailIgnoreCase(String email);

    /** Find by Google sub (Google account unique ID). */
    Optional<User> findByGoogleSub(String googleSub);

    /** Find by email (case-insensitive) OR Google sub. */
    Optional<User> findByEmailIgnoreCaseOrGoogleSub(String email, String googleSub);
}
