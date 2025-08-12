// src/main/java/com/nousware/service/BlogPostService.java
package com.nousware.service;

import com.nousware.entities.BlogPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Set;

public interface BlogPostService {

    // List all posts paginated
    Page<BlogPost> list(Pageable pageable);

    // Search by title
    Page<BlogPost> searchByTitle(String q, Pageable pageable);

    // Get by ID (throws if not found)
    BlogPost get(int id);

    // Get by slug (throws if not found)
    BlogPost getBySlug(String slug);

    // Create a post for userId and optional tags
    BlogPost create(String title, String content, String slug, int userId, Set<Integer> tagIds);

    // Update fields and optionally replace tags
    BlogPost update(int id, String title, String content, String slug, Set<Integer> tagIds);

    // Delete post and cascade deletes for comments/likes due to JPA mappings
    void delete(int id);

    // Add tags to a post
    BlogPost addTags(int postId, Set<Integer> tagIds);

    // Remove tags from a post
    BlogPost removeTags(int postId, Set<Integer> tagIds);

    // Like a post (idempotent)
    int like(int postId, int userId);

    // Unlike a post (no-op if not liked)
    int unlike(int postId, int userId);
}
