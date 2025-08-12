// src/main/java/com/nousware/service/impl/CommentLikeServiceImpl.java
package com.nousware.service.impl;

import com.nousware.entities.*;
import com.nousware.repository.*;
import com.nousware.service.CommentLikeService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service @Transactional
public class CommentLikeServiceImpl implements CommentLikeService {

    private final CommentLikeRepository likeRepo;
    private final CommentRepository commentRepo;
    private final UserRepository userRepo;

    public CommentLikeServiceImpl(CommentLikeRepository likeRepo, CommentRepository commentRepo, UserRepository userRepo) {
        this.likeRepo = likeRepo;
        this.commentRepo = commentRepo;
        this.userRepo = userRepo;
    }

    @Override
    public long like(int commentId, int userId) {
        Comment comment = commentRepo.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("Comment " + commentId + " not found"));
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User " + userId + " not found"));

        if (!likeRepo.existsByCommentAndUser(comment, user)) {
            CommentLike like = new CommentLike();
            like.setComment(comment);
            like.setUser(user);
            like.setCreatedAt(LocalDateTime.now());
            likeRepo.save(like);
        }
        return likeRepo.countByComment(comment);
    }

    @Override
    public long unlike(int commentId, int userId) {
        Comment comment = commentRepo.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("Comment " + commentId + " not found"));
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User " + userId + " not found"));

        likeRepo.findByCommentAndUser(comment, user).ifPresent(likeRepo::delete);
        return likeRepo.countByComment(comment);
    }

    @Override @Transactional(readOnly = true)
    public long countForComment(int commentId) {
        Comment c = commentRepo.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("Comment " + commentId + " not found"));
        return likeRepo.countByComment(c);
    }

    @Override @Transactional(readOnly = true)
    public Page<CommentLike> listByComment(int commentId, Pageable pageable) {
        return likeRepo.findByComment_CommentId(commentId, pageable);
    }

    @Override @Transactional(readOnly = true)
    public Page<CommentLike> listByUser(int userId, Pageable pageable) {
        return likeRepo.findByUser_UserId(userId, pageable);
    }
}
