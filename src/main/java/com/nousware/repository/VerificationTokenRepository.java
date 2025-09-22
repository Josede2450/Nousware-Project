package com.nousware.repository;

import com.nousware.entities.VerificationToken;
import com.nousware.enums.TokenType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Integer> {

    Optional<VerificationToken> findByToken(String token);

    List<VerificationToken> findByUser_UserId(Integer userId);

    /**
     * Hard-delete any existing tokens of a specific type for the given user.
     * Use this before inserting a fresh token to avoid (user_id, token_type) unique violations.
     *
     * @return number of rows deleted
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM VerificationToken v WHERE v.user.userId = :userId AND v.tokenType = :type")
    int deleteByUserIdAndType(Integer userId, TokenType type);
}
