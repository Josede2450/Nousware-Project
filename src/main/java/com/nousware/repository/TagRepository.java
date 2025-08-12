// src/main/java/com/nousware/repository/TagRepository.java
package com.nousware.repository;

import com.nousware.entities.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Integer> {

    // Find by unique slug
    Optional<Tag> findBySlug(String slug);

    // Check uniqueness constraint in code (add DB unique index too)
    boolean existsBySlug(String slug);

    // Simple name search
    Page<Tag> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
