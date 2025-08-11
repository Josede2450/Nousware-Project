package com.nousware.controller;

import com.nousware.entities.Faq;
import com.nousware.service.FaqService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/faqs")
public class FaqController {

    private final FaqService service;
    public FaqController(FaqService service) { this.service = service; }

    // Create — ADMIN only
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@Valid @RequestBody Faq body) {
        Faq created = service.create(body);
        Map<String, Object> resp = new HashMap<>();
        resp.put("message", "FAQ created successfully");
        resp.put("faq", created);
        return ResponseEntity.status(201).body(resp);
    }

    // Read one — public
    @GetMapping("/{id}")
    public ResponseEntity<Faq> get(@PathVariable Integer id) {
        return ResponseEntity.ok(service.get(id));
    }

    // List/search — public (?q=keyword) with pagination
    @GetMapping
    public ResponseEntity<Page<Faq>> list(@RequestParam(required = false) String q, Pageable pageable) {
        return ResponseEntity.ok(service.list(q, pageable));
    }

    // Update — ADMIN only
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(@PathVariable Integer id, @Valid @RequestBody Faq body) {
        Faq updated = service.update(id, body);
        Map<String, Object> resp = new HashMap<>();
        resp.put("message", "FAQ updated successfully");
        resp.put("faq", updated);
        return ResponseEntity.ok(resp);
    }

    // Delete — ADMIN only
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Integer id) {
        service.delete(id);
        Map<String, String> resp = Map.of("message", "FAQ deleted successfully");
        return ResponseEntity.ok(resp);
    }
}
