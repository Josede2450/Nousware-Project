package com.nousware.repository;

import com.nousware.entities.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceRepository extends JpaRepository<Service, Integer> {
    Page<Service> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String t, String d, Pageable pageable);
    boolean existsByTitleIgnoreCase(String title);
}
