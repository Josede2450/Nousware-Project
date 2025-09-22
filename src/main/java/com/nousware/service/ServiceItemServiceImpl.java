package com.nousware.service; // Package for service-layer classes

import com.nousware.entities.Category;
import com.nousware.entities.Service; // Import your JPA entity
import com.nousware.repository.CategoryRepository;
import com.nousware.repository.ServiceRepository; // Import JPA repository for Service
import org.springframework.data.domain.Page; // Paging support
import org.springframework.data.domain.Pageable; // Paging request
import org.springframework.http.HttpStatus; // HTTP status codes for errors
import org.springframework.transaction.annotation.Transactional; // Transaction management
import org.springframework.web.server.ResponseStatusException; // Throw web-friendly errors

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service // Mark this as a Spring Service bean (avoid clash with entity name "Service")
public class ServiceItemServiceImpl implements ServiceItemService {

    private final ServiceRepository repo;          // Repository dependency
    private final CategoryRepository categoryRepo; // NEW: to resolve incoming category ids

    // Constructor injection
    public ServiceItemServiceImpl(ServiceRepository repo, CategoryRepository categoryRepo) {
        this.repo = repo;
        this.categoryRepo = categoryRepo;
    }

    @Transactional
    @Override
    public Service create(Service input) {
        // Required
        String title = sanitizeRequired(input.getTitle(), "Title is required");
        String desc  = sanitizeRequired(input.getDescription(), "Description is required");

        // Optional
        String summary    = sanitizeOptional(input.getSummary());
        String imageUrl   = sanitizeOptional(input.getImageUrl());
        String priceRange = sanitizeOptionalMax(input.getPriceRange(), 250, "priceRange must be ≤ 250 characters"); // NEW
        String duration   = sanitizeOptionalMax(input.getDuration(),   255, "duration must be ≤ 255 characters");   // NEW

        // Unique title
        if (repo.existsByTitleIgnoreCase(title)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Title already exists");
        }

        // Build
        Service s = new Service();
        s.setTitle(title);
        s.setDescription(desc);
        s.setSummary(summary);
        s.setImageUrl(imageUrl);
        s.setMostPopular(input.isMostPopular());
        s.setPriceRange(priceRange); // NEW
        s.setDuration(duration);     // NEW

        // If client sent categories (id-only or full), resolve & set
        if (input.getCategories() != null && !input.getCategories().isEmpty()) {
            s.setCategories(resolveCategoriesFromInput(input.getCategories()));
        }

        return repo.save(s);
    }

    @Transactional
    @Override
    public Service update(Integer id, Service input) {
        Service existing = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service not found"));

        // Required validations
        String title = sanitizeRequired(input.getTitle(), "Title is required");
        String desc  = sanitizeRequired(input.getDescription(), "Description is required");

        // Optionals
        String summary    = sanitizeOptional(input.getSummary());
        String imageUrl   = sanitizeOptional(input.getImageUrl());
        String priceRange = sanitizeOptionalMax(input.getPriceRange(), 250, "priceRange must be ≤ 250 characters"); // NEW
        String duration   = sanitizeOptionalMax(input.getDuration(),   255, "duration must be ≤ 255 characters");   // NEW

        // Unique title on change
        if (!existing.getTitle().equalsIgnoreCase(title) && repo.existsByTitleIgnoreCase(title)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Title already exists");
        }

        // Apply updates
        existing.setTitle(title);
        existing.setDescription(desc);
        existing.setSummary(summary);
        existing.setImageUrl(imageUrl);
        existing.setMostPopular(input.isMostPopular());
        existing.setPriceRange(priceRange); // NEW
        existing.setDuration(duration);     // NEW

        // If categories provided, replace the set
        if (input.getCategories() != null) {
            if (input.getCategories().isEmpty()) {
                existing.getCategories().clear(); // explicit empty -> clear all
            } else {
                existing.setCategories(resolveCategoriesFromInput(input.getCategories()));
            }
        }

        return repo.save(existing);
    }

    @Transactional
    @Override
    public void delete(Integer id) {
        if (!repo.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Service not found");
        }
        repo.deleteById(id);
    }

    @Transactional(readOnly = true)
    @Override
    public Service get(Integer id) {
        // If you added @EntityGraph to repo.findById equivalent, switch to that for eager categories.
        return repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service not found"));
    }

    @Transactional(readOnly = true)
    @Override
    public Page<Service> list(String q, String categorySlug, Pageable pageable) {
        boolean hasQ = q != null && !q.isBlank();
        boolean hasCat = categorySlug != null && !categorySlug.isBlank();

        if (hasQ && hasCat) {
            return repo.findByCategorySlug(categorySlug.trim(), pageable);
        } else if (hasCat) {
            return repo.findByCategorySlug(categorySlug.trim(), pageable);
        } else if (hasQ) {
            String kw = q.trim();
            return repo.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(kw, kw, pageable);
        }
        return repo.findAll(pageable);
    }

    // === Helpers ===

    private String sanitizeRequired(String s, String message) {
        if (s == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        String t = s.trim();
        if (t.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        return t;
    }

    private String sanitizeOptional(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    // NEW: length-constrained optional
    private String sanitizeOptionalMax(String s, int max, String tooLongMsg) {
        String v = sanitizeOptional(s);
        if (v != null && v.length() > max) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, tooLongMsg);
        }
        return v;
    }

    /**
     * Resolve categories coming from the client.
     * Expecting array like: { "categories": [ { "categoryId": 1 }, { "categoryId": 3 } ] }
     * Only the IDs matter; we fetch canonical Category entities.
     */
    private Set<Category> resolveCategoriesFromInput(Set<Category> incoming) {
        // Collect requested IDs
        List<Integer> ids = incoming.stream()
                .map(Category::getCategoryId)
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();

        if (ids.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "categories must contain valid categoryId values");
        }

        List<Category> found = categoryRepo.findAllById(ids);
        if (found.size() != ids.size()) {
            // Find which ids are missing to give a clear error
            Set<Integer> foundIds = found.stream().map(Category::getCategoryId).collect(Collectors.toSet());
            List<Integer> missing = ids.stream().filter(id -> !foundIds.contains(id)).toList();
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Categories not found: " + missing);
        }

        return new HashSet<>(found);
    }
}
