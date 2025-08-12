// src/main/java/com/nousware/repository/BlogPostRepository.java
package com.nousware.repository;

import com.nousware.entities.BlogPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BlogPostRepository extends JpaRepository<BlogPost, Integer> {

    // Find a post by its unique slug
    Optional<BlogPost> findBySlug(String slug);

    // Search titles (simple contains, case-insensitive if your DB collation supports it)
    Page<BlogPost> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    // Ensure slug uniqueness checks without fetching the whole entity
    boolean existsBySlug(String slug);

    Page<BlogPost> findByTags_TagId(int tagId, Pageable pageable);
    Page<BlogPost> findByTags_Slug(String slug, Pageable pageable);
}
