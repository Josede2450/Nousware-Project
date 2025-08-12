package com.nousware.service;

import com.nousware.entities.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProjectService {
    Project create(Project input);                 // ADMIN only
    Project update(Integer id, Project input);     // ADMIN only
    void delete(Integer id);                       // ADMIN only
    Project get(Integer id);                       // Owner or ADMIN
    Page<Project> list(Pageable pageable);         // Owner gets own; ADMIN gets all
}
