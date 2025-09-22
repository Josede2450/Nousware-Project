package com.nousware.entities;

import com.nousware.enums.TokenType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "verification_token",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_verif_token__token", columnNames = "token"),
                @UniqueConstraint(name = "uk_verif_token__user_type", columnNames = {"user_id", "token_type"})
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VerificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "token_id")
    private int tokenId;

    @Column(nullable = false, unique = true, length = 128)
    private String token;

    @Column(name = "expiry_date", nullable = false)
    private LocalDateTime expiryDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "token_type", nullable = false, length = 32)
    private TokenType tokenType;

    // IMPORTANT: ManyToOne (not OneToOne) to avoid unique constraint on user_id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
