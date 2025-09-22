package com.nousware.service;

import com.nousware.dto.ViewTestimonial;
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
    private final UserServiceImpl userService; // for display picture

    public TestimonialServiceImpl(TestimonialRepository repo, UserRepository userRepo, UserServiceImpl userService) {
        this.repo = repo;
        this.userRepo = userRepo;
        this.userService = userService;
    }

    @Transactional
    @Override
    public Testimonial create(Testimonial input) {
        String content = sanitize(input.getContent());

        Testimonial t = new Testimonial();
        t.setContent(content);
        t.setCreatedAt(LocalDateTime.now());

        // optional user association
        Integer userId = (input.getUser() != null) ? input.getUser().getUserId() : null;
        if (userId != null && userId > 0) {
            User u = userRepo.findById(userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "User not found"));
            t.setUser(u);
        }

        // favorites: many allowed, no clearing of others
        t.setFavorite(input.isFavorite());

        return repo.save(t);
    }

    @Transactional
    @Override
    public Testimonial update(Integer id, Testimonial input) {
        Testimonial existing = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Testimonial not found"));

        existing.setContent(sanitize(input.getContent()));

        // optional user association
        Integer userId = (input.getUser() != null) ? input.getUser().getUserId() : null;
        if (userId != null && userId > 0) {
            User u = userRepo.findById(userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "User not found"));
            existing.setUser(u);
        } else if (input.getUser() == null) {
            existing.setUser(null); // allow clearing
        }

        existing.setFavorite(input.isFavorite());

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

    // -------- Lists (entities) --------
    @Transactional(readOnly = true)
    public Page<Testimonial> list(String q, Integer userId, Boolean favorite, Pageable pageable) {
        if (Boolean.TRUE.equals(favorite)) {
            return repo.findByFavoriteTrue(pageable);
        }
        if (userId != null) {
            return repo.findByUser_UserId(userId, pageable);
        }
        if (q != null && !q.isBlank()) {
            return repo.findByContentContainingIgnoreCase(q.trim(), pageable);
        }
        return repo.findAll(pageable);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<Testimonial> list(String q, Integer userId, Pageable pageable) {
        return list(q, userId, null, pageable);
    }

    // -------- View (DTO) helpers --------
    @Transactional(readOnly = true)
    public ViewTestimonial getView(Integer id) {
        return toView(get(id));
    }

    @Transactional(readOnly = true)
    public Page<ViewTestimonial> listView(String q, Integer userId, Boolean favorite, Pageable pageable) {
        return list(q, userId, favorite, pageable).map(this::toView);
    }

    @Transactional(readOnly = true)
    public Page<ViewTestimonial> listView(String q, Integer userId, Pageable pageable) {
        return list(q, userId, pageable).map(this::toView);
    }

    public ViewTestimonial toView(Testimonial t) {
        User u = t.getUser();
        ViewTestimonial.ViewUser vu = null;
        if (u != null) {
            String first = emptyToNull(u.getName());      // adjust if your field is firstName
            String last  = emptyToNull(u.getLastName());
            String picture = userService.getDisplayPictureUrl(u);
            vu = new ViewTestimonial.ViewUser(u.getUserId(), first, last, picture);
        }

        return new ViewTestimonial(
                t.getTestimonialId(),
                t.getContent(),       // DTO uses 'quote'
                t.getCreatedAt(),
                vu,
                t.isFavorite()        // ✅ include favorite
        );
    }

    // -------- helpers --------
    private String sanitize(String s) {
        if (s == null || s.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Content is required");
        }
        return s.trim();
    }

    private String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
