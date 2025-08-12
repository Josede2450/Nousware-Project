// src/main/java/com/nousware/repository/CommentLikeRepository.java
package com.nousware.repository;

import com.nousware.entities.CommentLike;
import com.nousware.entities.Comment;
import com.nousware.entities.User;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {
    boolean existsByCommentAndUser(Comment comment, User user);
    Optional<CommentLike> findByCommentAndUser(Comment comment, User user);
    long countByComment(Comment comment);
    Page<CommentLike> findByComment_CommentId(int commentId, Pageable pageable);
    Page<CommentLike> findByUser_UserId(int userId, Pageable pageable);
}
