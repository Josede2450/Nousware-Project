package com.nousware.repository;

import com.nousware.entities.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Integer> {
    Page<Project> findAllByUser_UserId(Integer userId, Pageable pageable);
}
