package com.nousware.service;

import com.nousware.entities.Testimonial;
import com.nousware.entities.User;
import com.nousware.repository.TestimonialRepository;
import com.nousware.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
public class TestimonialServiceImpl implements TestimonialService {

    private final TestimonialRepository repo;
    private final UserRepository userRepo;

    public TestimonialServiceImpl(TestimonialRepository repo, UserRepository userRepo) {
        this.repo = repo;
        this.userRepo = userRepo;
    }

    @Transactional
    @Override
    public Testimonial create(Testimonial input) {
        String content = sanitize(input.getContent());

        Testimonial t = new Testimonial();
        t.setContent(content);
        t.setCreatedAt(LocalDateTime.now());

        // Allow assigning a user by id via nested object: { "user": { "userId": 123 } }
        if (input.getUser() != null && input.getUser().getUserId() > 0) {
            User u = userRepo.findById(input.getUser().getUserId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "User not found"));
            t.setUser(u);
        }

        return repo.save(t);
    }

    @Transactional
    @Override
    public Testimonial update(Integer id, Testimonial input) {
        Testimonial existing = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Testimonial not found"));

        String content = sanitize(input.getContent());
        existing.setContent(content);

        // Optionally reassign to a different user
        if (input.getUser() != null && input.getUser().getUserId() > 0) {
            User u = userRepo.findById(input.getUser().getUserId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "User not found"));
            existing.setUser(u);
        }

        // keep original createdAt; update not tracked in this entity
        return repo.save(existing);
    }

    @Transactional
    @Override
    public void delete(Integer id) {
        if (!repo.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Testimonial not found");
        repo.deleteById(id);
    }

    @Transactional(readOnly = true)
    @Override
    public Testimonial get(Integer id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Testimonial not found"));
    }

    @Transactional(readOnly = true)
    @Override
    public Page<Testimonial> list(String q, Integer userId, Pageable pageable) {
        if (userId != null) {
            return repo.findByUser_UserId(userId, pageable);
        }
        if (q != null && !q.isBlank()) {
            return repo.findByContentContainingIgnoreCase(q.trim(), pageable);
        }
        return repo.findAll(pageable);
    }

    private String sanitize(String s) {
        if (s == null || s.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Content is required");
        }
        return s.trim();
    }
}
