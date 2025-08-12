// src/main/java/com/nousware/service/impl/BlogPostServiceImpl.java
package com.nousware.service.impl;

import com.nousware.entities.BlogPost;
import com.nousware.entities.PostLike;
import com.nousware.entities.Tag;
import com.nousware.entities.User;
import com.nousware.repository.BlogPostRepository;
import com.nousware.repository.PostLikeRepository;
import com.nousware.repository.TagRepository;
import com.nousware.repository.UserRepository;
import com.nousware.service.BlogPostService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Service
@Transactional
public class BlogPostServiceImpl implements BlogPostService {

    private final BlogPostRepository blogPostRepo;
    private final UserRepository userRepo;
    private final TagRepository tagRepo;
    private final PostLikeRepository likeRepo;

    public BlogPostServiceImpl(
            BlogPostRepository blogPostRepo,
            UserRepository userRepo,
            TagRepository tagRepo,
            PostLikeRepository likeRepo
    ) {
        this.blogPostRepo = blogPostRepo;
        this.userRepo = userRepo;
        this.tagRepo = tagRepo;
        this.likeRepo = likeRepo;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BlogPost> list(Pageable pageable) {
        return blogPostRepo.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BlogPost> searchByTitle(String q, Pageable pageable) {
        return blogPostRepo.findByTitleContainingIgnoreCase(q == null ? "" : q, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public BlogPost get(int id) {
        return blogPostRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("BlogPost " + id + " not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public BlogPost getBySlug(String slug) {
        return blogPostRepo.findBySlug(slug)
                .orElseThrow(() -> new EntityNotFoundException("BlogPost with slug '" + slug + "' not found"));
    }

    @Override
    public BlogPost create(String title, String content, String slug, int userId, Set<Integer> tagIds) {
        if (blogPostRepo.existsBySlug(slug)) {
            throw new IllegalArgumentException("Slug already in use: " + slug);
        }

        User author = userRepo.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User " + userId + " not found"));

        BlogPost post = new BlogPost();
        post.setTitle(title);
        post.setContent(content);
        post.setSlug(slug);
        post.setUser(author);
        post.setCreatedAt(LocalDateTime.now());
        post.setUpdatedAt(LocalDateTime.now());

        if (tagIds != null && !tagIds.isEmpty()) {
            Set<Tag> tags = new HashSet<>(tagRepo.findAllById(tagIds));
            post.setTags(tags);
        }

        return blogPostRepo.save(post);
    }

    @Override
    public BlogPost update(int id, String title, String content, String slug, Set<Integer> tagIds) {
        BlogPost post = get(id);

        if (slug != null && !slug.equals(post.getSlug())) {
            if (blogPostRepo.existsBySlug(slug)) {
                throw new IllegalArgumentException("Slug already in use: " + slug);
            }
            post.setSlug(slug);
        }

        if (title != null) post.setTitle(title);
        if (content != null) post.setContent(content);
        if (tagIds != null) {
            Set<Tag> newTags = new HashSet<>(tagRepo.findAllById(tagIds));
            post.setTags(newTags); // full replacement if provided
        }

        post.setUpdatedAt(LocalDateTime.now());
        return blogPostRepo.save(post);
    }

    @Override
    public void delete(int id) {
        BlogPost post = get(id);
        blogPostRepo.delete(post); // comments/likes removed by cascade
    }

    @Override
    public BlogPost addTags(int postId, Set<Integer> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) return get(postId);
        BlogPost post = get(postId);
        Set<Tag> tags = post.getTags() == null ? new HashSet<>() : new HashSet<>(post.getTags());
        tags.addAll(tagRepo.findAllById(tagIds));
        post.setTags(tags);
        post.setUpdatedAt(LocalDateTime.now());
        return blogPostRepo.save(post);
    }

    @Override
    public BlogPost removeTags(int postId, Set<Integer> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) return get(postId);
        BlogPost post = get(postId);
        if (post.getTags() != null) {
            post.getTags().removeIf(t -> tagIds.contains(t.getTagId()));
        }
        post.setUpdatedAt(LocalDateTime.now());
        return blogPostRepo.save(post);
    }

    @Override
    public int like(int postId, int userId) {
        BlogPost post = get(postId);
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User " + userId + " not found"));

        if (!likeRepo.existsByBlogPostAndUser(post, user)) {
            PostLike like = new PostLike();
            like.setBlogPost(post);
            like.setUser(user);
            likeRepo.save(like);
        }

        return (int) likeRepo.countByBlogPost(post);
    }

    @Override
    public int unlike(int postId, int userId) {
        BlogPost post = get(postId);
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User " + userId + " not found"));

        likeRepo.findByBlogPostAndUser(post, user).ifPresent(likeRepo::delete);

        return (int) likeRepo.countByBlogPost(post);
    }
}
