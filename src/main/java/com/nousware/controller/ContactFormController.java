package com.nousware.controller;

import com.nousware.entities.ContactForm;
import com.nousware.service.ContactFormService;
import com.nousware.service.EmailService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(ContactFormController.class);

    private final ContactFormService service;
    private final EmailService emailService;

    public ContactFormController(ContactFormService service, EmailService emailService) {
        this.service = service;
        this.emailService = emailService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@Valid @RequestBody ContactForm body) {
        // Persist
        ContactForm created = service.create(body);

        // Send ONLY the user confirmation here
        try {
            emailService.sendContactConfirmation(created);
        } catch (Exception e) {
            log.warn("Contact confirmation email failed for id={}, to={}", created.getContactId(), created.getEmail(), e);
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("message", "Thanks! Your message has been sent. Check your email for confirmation.");
        resp.put("contact", created);
        return ResponseEntity.status(201).body(resp);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<ContactForm> get(@PathVariable Integer id) {
        return ResponseEntity.ok(service.get(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<Page<ContactForm>> list(@RequestParam(required = false) String q, Pageable pageable) {
        return ResponseEntity.ok(service.list(q, pageable));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Integer id) {
        service.delete(id);
        return ResponseEntity.ok(Map.of("message", "Contact message deleted"));
    }
}
