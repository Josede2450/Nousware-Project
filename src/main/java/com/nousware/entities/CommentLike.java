// src/main/java/com/nousware/entities/CommentLike.java
package com.nousware.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "comment_like",
        uniqueConstraints = @UniqueConstraint(name = "uk_comment_user_like", columnNames = {"comment_id","user_id"})
)
@Data @NoArgsConstructor @AllArgsConstructor
public class CommentLike {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "comment_id")
    private Comment comment;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    private LocalDateTime createdAt;
}
