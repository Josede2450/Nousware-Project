package com.nousware.controller;

import com.nousware.entities.Project;
import com.nousware.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService service;
    public ProjectController(ProjectService service) { this.service = service; }

    // Create — ADMIN only
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@Valid @RequestBody Project body) {
        Project created = service.create(body);
        Map<String, Object> resp = new HashMap<>();
        resp.put("message", "Project created successfully");
        resp.put("project", created);
        return ResponseEntity.status(201).body(resp);
    }

    // Read one — Owner or Admin
    @GetMapping("/{id}")
    public ResponseEntity<Project> get(@PathVariable Integer id) {
        return ResponseEntity.ok(service.get(id));
    }

    // List — Owner sees own; Admin sees all
    @GetMapping
    public ResponseEntity<Page<Project>> list(Pageable pageable) {
        return ResponseEntity.ok(service.list(pageable));
    }

    // Update — ADMIN only
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(@PathVariable Integer id, @Valid @RequestBody Project body) {
        Project updated = service.update(id, body);
        Map<String, Object> resp = new HashMap<>();
        resp.put("message", "Project updated successfully");
        resp.put("project", updated);
        return ResponseEntity.ok(resp);
    }

    // Delete — ADMIN only
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Integer id) {
        service.delete(id);
        return ResponseEntity.ok(Map.of("message", "Project deleted successfully"));
    }
}
