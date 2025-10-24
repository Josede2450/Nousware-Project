package com.nousware.service;

import com.nousware.dto.ViewTestimonial;
import com.nousware.entities.Testimonial;
import com.nousware.repository.TestimonialRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.time.LocalDateTime;

@Service
public class TestimonialServiceImpl implements TestimonialService {

    private final TestimonialRepository repo;

    public TestimonialServiceImpl(TestimonialRepository repo) {
        this.repo = repo;
    }

    /* ==================== CREATE ==================== */
    @Transactional
    @Override
    public Testimonial create(Testimonial input) {
        String content = requireText(input.getContent(), "Content is required");

        Testimonial t = new Testimonial();
        t.setContent(content);
        t.setCreatedAt(LocalDateTime.now());
        t.setFavorite(input.isFavorite());
        t.setImgUrl(sanitizeUrl(input.getImgUrl()));

        return repo.save(t);
    }

    /* ==================== UPDATE ==================== */
    @Transactional
    @Override
    public Testimonial update(Integer id, Testimonial input) {
        Testimonial existing = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Testimonial not found"));

        existing.setContent(requireText(input.getContent(), "Content is required"));
        existing.setFavorite(input.isFavorite());
        existing.setImgUrl(sanitizeUrl(input.getImgUrl()));

        return repo.save(existing);
    }

    /* ==================== DELETE ==================== */
    @Transactional
    @Override
    public void delete(Integer id) {
        if (!repo.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Testimonial not found");
        }
        repo.deleteById(id);
    }

    /* ==================== GET ONE ==================== */
    @Transactional(readOnly = true)
    @Override
    public Testimonial get(Integer id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Testimonial not found"));
    }

    /* ==================== LIST ENTITIES ==================== */
    @Transactional(readOnly = true)
    @Override
    public Page<Testimonial> list(String q, Boolean favorite, Pageable pageable) {
        if (Boolean.TRUE.equals(favorite)) {
            return repo.findByFavoriteTrue(pageable);
        }
        if (q != null && !q.isBlank()) {
            return repo.findByContentContainingIgnoreCase(q.trim(), pageable);
        }
        return repo.findAll(pageable);
    }

    /* ==================== LIST VIEW (DTO) ==================== */
    @Transactional(readOnly = true)
    @Override
    public Page<ViewTestimonial> listView(String q, Boolean favorite, Pageable pageable) {
        return list(q, favorite, pageable).map(this::toView);
    }

    @Transactional(readOnly = true)
    @Override
    public ViewTestimonial getView(Integer id) {
        return toView(get(id));
    }

    /* ==================== MAPPING HELPER ==================== */
    private ViewTestimonial toView(Testimonial t) {
        return new ViewTestimonial(
                t.getTestimonialId(),
                t.getContent(),
                t.getCreatedAt(),
                t.isFavorite(),
                t.getImgUrl() // ✅ include image URL
        );
    }

    /* ==================== VALIDATION HELPERS ==================== */
    private String requireText(String s, String message) {
        if (s == null || s.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return s.trim();
    }

    private String sanitizeUrl(String url) {
        if (url == null || url.isBlank()) return null;
        String trimmed = url.trim();
        try {
            URI u = URI.create(trimmed);
            if (u.getScheme() == null || u.getHost() == null) return null;
            return trimmed;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
