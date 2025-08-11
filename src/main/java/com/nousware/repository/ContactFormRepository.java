package com.nousware.repository;

import com.nousware.entities.ContactForm;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactFormRepository extends JpaRepository<ContactForm, Integer> {
    Page<ContactForm> findByNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrMessageContainingIgnoreCase(
            String name, String email, String message, Pageable pageable);
}
