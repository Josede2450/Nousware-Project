package com.nousware.repository;

import com.nousware.entities.Testimonial;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TestimonialRepository extends JpaRepository<Testimonial, Integer> {

    // 🔍 Search by content (case-insensitive)
    Page<Testimonial> findByContentContainingIgnoreCase(String q, Pageable pageable);

    // ⭐ Only favorites
    Page<Testimonial> findByFavoriteTrue(Pageable pageable);

    // 🕒 Optional: latest favorite for highlight section
    Optional<Testimonial> findFirstByFavoriteTrueOrderByCreatedAtDesc();
}
