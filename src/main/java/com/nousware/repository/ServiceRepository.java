package com.nousware.repository;

import com.nousware.entities.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ServiceRepository extends JpaRepository<Service, Integer> {

    // 🔹 When searching by title/description, also fetch categories
    @EntityGraph(attributePaths = "categories")
    Page<Service> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
            String t, String d, Pageable pageable);

    boolean existsByTitleIgnoreCase(String title);

    // 🔹 Default list should fetch categories
    @EntityGraph(attributePaths = "categories")
    Page<Service> findAll(Pageable pageable);

    // 🔹 By id with categories (use this in get())
    @EntityGraph(attributePaths = "categories")
    Optional<Service> findById(Integer id);

    // 🔹 Filter by category slug and fetch categories
    @EntityGraph(attributePaths = "categories")
    @Query("""
           SELECT DISTINCT s FROM Service s
           JOIN s.categories c
           WHERE LOWER(c.slug) = LOWER(:slug)
           """)
    Page<Service> findByCategorySlug(@Param("slug") String slug, Pageable pageable);
}
