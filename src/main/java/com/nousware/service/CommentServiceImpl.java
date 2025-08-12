// src/main/java/com/nousware/service/impl/CommentServiceImpl.java
package com.nousware.service.impl;

import com.nousware.entities.BlogPost;
import com.nousware.entities.Comment;
import com.nousware.entities.User;
import com.nousware.repository.BlogPostRepository;
import com.nousware.repository.CommentRepository;
import com.nousware.repository.UserRepository;
import com.nousware.service.CommentService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
public class CommentServiceImpl implements CommentService {

    private static final int MAX_REPLY_DEPTH = 5;

    private final CommentRepository commentRepo;
    private final BlogPostRepository postRepo;
    private final UserRepository userRepo;

    public CommentServiceImpl(CommentRepository commentRepo, BlogPostRepository postRepo, UserRepository userRepo) {
        this.commentRepo = commentRepo;
        this.postRepo = postRepo;
        this.userRepo = userRepo;
    }

    @Override
    public Comment create(String content, int postId, int userId, Integer parentCommentId) {
        BlogPost post = postRepo.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("Post " + postId + " not found"));
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User " + userId + " not found"));

        Comment comment = new Comment();
        comment.setContent(content);
        comment.setCreatedAt(LocalDateTime.now());
        comment.setBlogPost(post);
        comment.setUser(user);

        // ----- Robust parent handling -----
        if (parentCommentId != null) {
            if (parentCommentId <= 0) {
                throw new IllegalArgumentException("parentCommentId must be a positive integer");
            }

            // Fast guard: ensure the parent exists AND belongs to the same post
            boolean parentInSamePost = commentRepo.existsByCommentIdAndBlogPost_PostId(parentCommentId, postId);
            if (!parentInSamePost) {
                // Clarify the failure reason
                commentRepo.findById(parentCommentId)
                        .orElseThrow(() -> new EntityNotFoundException("Parent comment " + parentCommentId + " not found"));
                throw new IllegalArgumentException(
                        "Parent comment %d belongs to a different post than %d".formatted(parentCommentId, postId));
            }

            Comment parent = commentRepo.findById(parentCommentId).get(); // safe due to guard above

            // Optional: limit nesting depth to keep threads manageable
            if (depth(parent) >= MAX_REPLY_DEPTH) {
                throw new IllegalArgumentException("Maximum reply depth reached (" + MAX_REPLY_DEPTH + ").");
            }

            comment.setParentComment(parent);
        }
        // ----------------------------------

        return commentRepo.save(comment);
    }

    @Override
    public Comment update(int commentId, String content) {
        Comment comment = commentRepo.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("Comment " + commentId + " not found"));
        comment.setContent(content);
        return commentRepo.save(comment);
    }

    @Override
    public void delete(int commentId) {
        Comment comment = commentRepo.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("Comment " + commentId + " not found"));
        commentRepo.delete(comment);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Comment> listByPost(int postId, Pageable pageable) {
        BlogPost post = postRepo.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("Post " + postId + " not found"));
        return commentRepo.findByBlogPost(post, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Comment> listTopLevelByPost(int postId, Pageable pageable) {
        BlogPost post = postRepo.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("Post " + postId + " not found"));
        return commentRepo.findByBlogPostAndParentCommentIsNull(post, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Comment> listByUser(int userId, Pageable pageable) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User " + userId + " not found"));
        return commentRepo.findByUser(user, pageable);
    }

    /** Compute nesting depth by walking up the chain. */
    private int depth(Comment c) {
        int d = 0;
        while (c != null) {
            d++;
            c = c.getParentComment();
            if (d > 32) break; // safety stop to avoid pathological chains
        }
        return d;
    }
}
