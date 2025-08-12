// src/main/java/com/nousware/repository/CommentRepository.java
package com.nousware.repository;

import com.nousware.entities.Comment;
import com.nousware.entities.BlogPost;
import com.nousware.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Integer> {

    // All comments for a post
    Page<Comment> findByBlogPost(BlogPost post, Pageable pageable);

    // Top-level comments (no parent)
    Page<Comment> findByBlogPostAndParentCommentIsNull(BlogPost post, Pageable pageable);

    // Comments by a specific user
    Page<Comment> findByUser(User user, Pageable pageable);

    boolean existsByCommentIdAndBlogPost_PostId(int commentId, int postId);

}
