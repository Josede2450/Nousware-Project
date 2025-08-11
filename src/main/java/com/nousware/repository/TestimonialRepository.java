package com.nousware.repository;

import com.nousware.entities.Testimonial;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestimonialRepository extends JpaRepository<Testimonial, Integer> {

    Page<Testimonial> findByContentContainingIgnoreCase(String q, Pageable pageable);

    Page<Testimonial> findByUser_UserId(Integer userId, Pageable pageable);
}
