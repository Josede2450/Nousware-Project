package com.nousware.repository;

import com.nousware.entities.Testimonial;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TestimonialRepository extends JpaRepository<Testimonial, Integer> {

    // Common list/find (eager-load user for table views)
    @EntityGraph(attributePaths = "user")
    Page<Testimonial> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "user")
    Page<Testimonial> findByContentContainingIgnoreCase(String q, Pageable pageable);

    @EntityGraph(attributePaths = "user")
    Page<Testimonial> findByUser_UserId(Integer userId, Pageable pageable);

    // ✅ Favorites (for ?favorite=true)
    @EntityGraph(attributePaths = "user")
    Page<Testimonial> findByFavoriteTrue(Pageable pageable);

    // (Optional) If you still want a quick “latest favorite” endpoint
    @EntityGraph(attributePaths = "user")
    Optional<Testimonial> findFirstByFavoriteTrueOrderByCreatedAtDesc();

    // ⛔️ Removed: single-best helpers
    // - findByBestOneTrue(...)
    // - findFirstByBestOneTrueOrderByCreatedAtDesc()
    // - clearBestOne()
}
