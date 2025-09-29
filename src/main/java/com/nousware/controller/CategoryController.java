package com.nousware.controller;

import com.nousware.entities.Category;
import com.nousware.service.CategoryItemService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryItemService service;

    public CategoryController(CategoryItemService service) {
        this.service = service;
    }

    // Create — ADMIN only
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@Valid @RequestBody Category body) {
        Category created = service.create(body);
        Map<String, Object> resp = new HashMap<>();
        resp.put("message", "Category created successfully");
        resp.put("category", created);
        return ResponseEntity.status(201).body(resp);
    }

    // Read one — public
    @GetMapping("/{id}")
    public ResponseEntity<Category> get(@PathVariable Integer id) {
        return ResponseEntity.ok(service.get(id));
    }

    // List/search — public (?q=keyword) with pagination
    @GetMapping
    public ResponseEntity<Page<Category>> list(
            @RequestParam(required = false) String q,
            Pageable pageable
    ) {
        return ResponseEntity.ok(service.list(q, pageable));
    }

    // Update — ADMIN only
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(
            @PathVariable Integer id,
            @Valid @RequestBody Category body
    ) {
        Category updated = service.update(id, body);
        Map<String, Object> resp = new HashMap<>();
        resp.put("message", "Category updated successfully");
        resp.put("category", updated);
        return ResponseEntity.ok(resp);
    }

    // Delete — ADMIN only
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Integer id) {
        service.delete(id);
        return ResponseEntity.ok(Map.of("message", "Category deleted successfully"));
    }
}
