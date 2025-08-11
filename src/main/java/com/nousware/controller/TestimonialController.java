package com.nousware.controller;

import com.nousware.entities.Testimonial;
import com.nousware.service.TestimonialService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/testimonials")
public class TestimonialController {

    private final TestimonialService service;

    public TestimonialController(TestimonialService service) {
        this.service = service;
    }

    // Create — ADMIN only
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@Valid @RequestBody Testimonial body) {
        Testimonial created = service.create(body);
        Map<String, Object> resp = new HashMap<>();
        resp.put("message", "Testimonial created successfully");
        resp.put("testimonial", created);
        return ResponseEntity.status(201).body(resp);
    }

    // Read one — public
    @GetMapping("/{id}")
    public ResponseEntity<Testimonial> get(@PathVariable Integer id) {
        return ResponseEntity.ok(service.get(id));
    }

    // List/search — public
    // Use ?q=keyword OR ?userId=123 (q takes precedence if both provided)
    @GetMapping
    public ResponseEntity<Page<Testimonial>> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer userId,
            Pageable pageable
    ) {
        return ResponseEntity.ok(service.list(q, userId, pageable));
    }

    // Update — ADMIN only
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(@PathVariable Integer id,
                                                      @Valid @RequestBody Testimonial body) {
        Testimonial updated = service.update(id, body);
        Map<String, Object> resp = new HashMap<>();
        resp.put("message", "Testimonial updated successfully");
        resp.put("testimonial", updated);
        return ResponseEntity.ok(resp);
    }

    // Delete — ADMIN only
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Integer id) {
        service.delete(id);
        return ResponseEntity.ok(Map.of("message", "Testimonial deleted successfully"));
    }
}
