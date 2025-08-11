package com.nousware.service;

import com.nousware.entities.ContactForm;
import com.nousware.repository.ContactFormRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
public class ContactFormServiceImpl implements ContactFormService {

    private final ContactFormRepository repo;
    private final EmailService emailService;

    public ContactFormServiceImpl(ContactFormRepository repo, EmailService emailService) {
        this.repo = repo;
        this.emailService = emailService;
    }

    @Transactional
    @Override
    public ContactForm create(ContactForm input) {
        if (isBlank(input.getName()))   throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name is required");
        if (isBlank(input.getEmail()))  throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required");
        if (isBlank(input.getMessage()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message is required");

        input.setName(input.getName().trim());
        input.setEmail(input.getEmail().trim().toLowerCase());
        if (input.getPhone() != null) input.setPhone(input.getPhone().trim());
        input.setMessage(input.getMessage().trim());
        input.setCreatedAt(LocalDateTime.now()); // <-- set here per your entity

        ContactForm saved = repo.save(input);

        // notify via email
        emailService.sendContactNotification(saved);

        return saved;
    }

    @Transactional(readOnly = true)
    @Override
    public ContactForm get(Integer id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contact form not found"));
    }

    @Transactional(readOnly = true)
    @Override
    public Page<ContactForm> list(String q, Pageable pageable) {
        if (q != null && !q.isBlank()) {
            String kw = q.trim();
            return repo.findByNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrMessageContainingIgnoreCase(
                    kw, kw, kw, pageable);
        }
        return repo.findAll(pageable);
    }

    @Transactional
    @Override
    public void delete(Integer id) {
        if (!repo.existsById(id))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Contact form not found");
        repo.deleteById(id);
    }

    private boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
}
