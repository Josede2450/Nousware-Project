// src/main/java/com/nousware/service/impl/PostLikeServiceImpl.java
package com.nousware.service.impl;

import com.nousware.entities.BlogPost;
import com.nousware.entities.PostLike;
import com.nousware.entities.User;
import com.nousware.repository.BlogPostRepository;
import com.nousware.repository.PostLikeRepository;
import com.nousware.repository.UserRepository;
import com.nousware.service.PostLikeService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
public class PostLikeServiceImpl implements PostLikeService {

    private final PostLikeRepository likeRepo;
    private final BlogPostRepository postRepo;
    private final UserRepository userRepo;

    public PostLikeServiceImpl(PostLikeRepository likeRepo,
                               BlogPostRepository postRepo,
                               UserRepository userRepo) {
        this.likeRepo = likeRepo;
        this.postRepo = postRepo;
        this.userRepo = userRepo;
    }

    @Override
    public long like(int postId, int userId) {
        BlogPost post = postRepo.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("Post " + postId + " not found"));
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User " + userId + " not found"));

        // Idempotent: only create if not already liked
        if (!likeRepo.existsByBlogPostAndUser(post, user)) {
            PostLike like = new PostLike();
            like.setBlogPost(post);
            like.setUser(user);
            like.setCreatedAt(LocalDateTime.now());
            likeRepo.save(like);
        }
        return likeRepo.countByBlogPost(post);
    }

    @Override
    public long unlike(int postId, int userId) {
        BlogPost post = postRepo.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("Post " + postId + " not found"));
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User " + userId + " not found"));

        likeRepo.findByBlogPostAndUser(post, user).ifPresent(likeRepo::delete);
        return likeRepo.countByBlogPost(post);
    }

    @Override
    @Transactional(readOnly = true)
    public long countForPost(int postId) {
        BlogPost post = postRepo.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("Post " + postId + " not found"));
        return likeRepo.countByBlogPost(post);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PostLike> listByPost(int postId, Pageable pageable) {
        return likeRepo.findByBlogPost_PostId(postId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PostLike> listByUser(int userId, Pageable pageable) {
        return likeRepo.findByUser_UserId(userId, pageable);
    }
}
