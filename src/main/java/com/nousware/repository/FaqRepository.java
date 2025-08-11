package com.nousware.repository;

import com.nousware.entities.Faq;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FaqRepository extends JpaRepository<Faq, Integer> {
    // Simple keyword search across question and answer (case-insensitive)
    Page<Faq> findByQuestionContainingIgnoreCaseOrAnswerContainingIgnoreCase(String q, String a, Pageable pageable);

    // Optional: prevent duplicate questions if you want that business rule
    boolean existsByQuestionIgnoreCase(String question);
}
