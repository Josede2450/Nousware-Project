package com.nousware.repository;

import com.nousware.entities.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Integer> {
    Optional<Category> findBySlug(String slug);
    boolean existsBySlugIgnoreCase(String slug);
    Optional<Category> findByNameIgnoreCase(String name);
}
