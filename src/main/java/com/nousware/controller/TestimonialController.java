package com.nousware.controller;

import com.nousware.dto.ViewTestimonial;
import com.nousware.entities.Testimonial;
import com.nousware.service.TestimonialServiceImpl;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping(value = "/api/testimonials", produces = MediaType.APPLICATION_JSON_VALUE)
public class TestimonialController {

    private final TestimonialServiceImpl service;

    public TestimonialController(TestimonialServiceImpl service) {
        this.service = service;
    }

    /* ---------- CREATE (Admin only) ---------- */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ViewTestimonial> create(@Valid @RequestBody Testimonial body) {
        Testimonial created = service.create(body);
        return ResponseEntity.status(201).body(service.getView(created.getTestimonialId()));
    }

    /* ---------- READ ONE (Public) ---------- */
    @GetMapping("/{id}")
    public ResponseEntity<ViewTestimonial> get(@PathVariable Integer id) {
        return ResponseEntity.ok(service.getView(id));
    }

    /* ---------- LIST / SEARCH (Public) ---------- */
    // Supports ?q=keyword, ?favorite=true, and pageable params (?page=&size=&sort=createdAt,desc)
    @GetMapping
    public ResponseEntity<Page<ViewTestimonial>> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean favorite,
            Pageable pageable
    ) {
        return ResponseEntity.ok(service.listView(q, favorite, pageable));
    }

    /* ---------- UPDATE (Admin only) ---------- */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ViewTestimonial> update(@PathVariable Integer id,
                                                  @Valid @RequestBody Testimonial body) {
        Testimonial updated = service.update(id, body);
        return ResponseEntity.ok(service.getView(updated.getTestimonialId()));
    }

    /* ---------- DELETE (Admin only) ---------- */
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Integer id) {
        service.delete(id);
        return ResponseEntity.ok(Map.of("message", "Testimonial deleted successfully"));
    }
}
