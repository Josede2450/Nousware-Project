package com.nousware.controller;

import com.nousware.entities.ContactForm;
import com.nousware.service.ContactFormService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/contact")
public class ContactFormController {

    private final ContactFormService service;

    public ContactFormController(ContactFormService service) {
        this.service = service;
    }

    // Public submit
    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@Valid @RequestBody ContactForm body) {
        ContactForm created = service.create(body);
        Map<String, Object> resp = new HashMap<>();
        resp.put("message", "Thanks! Your message has been received.");
        resp.put("contact", created);
        return ResponseEntity.status(201).body(resp);
    }

    // Admin: get one
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<ContactForm> get(@PathVariable Integer id) {
        return ResponseEntity.ok(service.get(id));
    }

    // Admin: list & search
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<Page<ContactForm>> list(@RequestParam(required = false) String q, Pageable pageable) {
        return ResponseEntity.ok(service.list(q, pageable));
    }

    // Admin: delete
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Integer id) {
        service.delete(id);
        return ResponseEntity.ok(Map.of("message", "Contact message deleted"));
    }
}
