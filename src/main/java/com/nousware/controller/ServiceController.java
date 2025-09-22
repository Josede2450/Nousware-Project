package com.nousware.controller;

import com.nousware.entities.Service;
import com.nousware.service.ServiceItemService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/services")
public class ServiceController {

    private final ServiceItemService service;
    public ServiceController(ServiceItemService service) { this.service = service; }

    // Create — ADMIN only
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@Valid @RequestBody Service body) {
        Service created = service.create(body);
        Map<String, Object> resp = new HashMap<>();
        resp.put("message", "Service created successfully");
        resp.put("service", created);
        return ResponseEntity.status(201).body(resp);
    }

    // Read one — public
    @GetMapping("/{id}")
    public ResponseEntity<Service> get(@PathVariable Integer id) {
        return ResponseEntity.ok(service.get(id));
    }

    // List/search — public (?q=keyword&category=slug) with pagination
    @GetMapping
    public ResponseEntity<Page<Service>> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            Pageable pageable
    ) {
        return ResponseEntity.ok(service.list(q, category, pageable));
    }

    // Update — ADMIN only
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(@PathVariable Integer id, @Valid @RequestBody Service body) {
        Service updated = service.update(id, body);
        Map<String, Object> resp = new HashMap<>();
        resp.put("message", "Service updated successfully");
        resp.put("service", updated);
        return ResponseEntity.ok(resp);
    }

    // Delete — ADMIN only
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Integer id) {
        service.delete(id);
        return ResponseEntity.ok(Map.of("message", "Service deleted successfully"));
    }
}
