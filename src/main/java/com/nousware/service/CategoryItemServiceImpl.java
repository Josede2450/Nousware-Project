package com.nousware.service;

import com.nousware.entities.Category;
import com.nousware.repository.CategoryRepository;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
public class CategoryItemServiceImpl implements CategoryItemService {

    private final CategoryRepository repo;

    public CategoryItemServiceImpl(CategoryRepository repo) {
        this.repo = repo;
    }

    @Transactional
    @Override
    public Category create(Category input) {
        // === Validate ===
        String name = requireNonBlank(input.getName(), "Name is required");

        // If slug not provided, derive from name
        String slug = sanitizeSlug(input.getSlug());
        if (slug == null || slug.isBlank()) {
            slug = slugify(name);
        }

        // Unique slug check (case-insensitive)
        if (repo.existsBySlugIgnoreCase(slug)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Slug already exists: " + slug);
        }

        // Persist
        Category c = new Category();
        c.setName(name.trim());
        c.setSlug(slug);
        return repo.save(c);
    }

    @Transactional
    @Override
    public Category update(Integer id, Category input) {
        Category existing = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));

        String name = requireNonBlank(input.getName(), "Name is required");

        // Compute desired slug (prefer payload slug; otherwise derive from new name)
        String desiredSlug = sanitizeSlug(input.getSlug());
        if (desiredSlug == null || desiredSlug.isBlank()) {
            desiredSlug = slugify(name);
        }

        // If slug changed, enforce uniqueness
        String currentSlug = existing.getSlug();
        if (!equalsIgnoreCaseSafe(currentSlug, desiredSlug) && repo.existsBySlugIgnoreCase(desiredSlug)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Slug already exists: " + desiredSlug);
        }

        existing.setName(name.trim());
        existing.setSlug(desiredSlug);
        return repo.save(existing);
    }

    @Transactional
    @Override
    public void delete(Integer id) {
        if (!repo.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found");
        }
        repo.deleteById(id);
        // If you want FK cascade from service_category, make sure the join table has ON DELETE CASCADE
    }

    @Transactional(readOnly = true)
    @Override
    public Category get(Integer id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
    }

    @Transactional(readOnly = true)
    @Override
    public Page<Category> list(String q, Pageable pageable) {
        // If you add a repo method like:
        // Page<Category> findByNameContainingIgnoreCaseOrSlugContainingIgnoreCase(String n, String s, Pageable p);
        // you can use it here. Until then, do a safe in-memory filter with proper paging.
        if (q == null || q.isBlank()) {
            return repo.findAll(pageable);
        }

        String kw = q.trim().toLowerCase(Locale.ROOT);

        // Note: repo.findAll() returns a List; we'll page it manually
        List<Category> all = repo.findAll();
        List<Category> filtered = all.stream()
                .filter(c -> {
                    String n = safeLower(c.getName());
                    String s = safeLower(c.getSlug());
                    return (n != null && n.contains(kw)) || (s != null && s.contains(kw));
                })
                .collect(Collectors.toList());

        int total = filtered.size();
        int page = pageable.getPageNumber();
        int size = pageable.getPageSize();
        int from = Math.min(page * size, total);
        int to = Math.min(from + size, total);
        List<Category> content = filtered.subList(from, to);

        return new PageImpl<>(content, pageable, total);
    }

    // ===== Helpers =====

    private static String requireNonBlank(String s, String message) {
        if (s == null || s.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return s;
    }

    private static String sanitizeSlug(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : slugify(t);
    }

    private static boolean equalsIgnoreCaseSafe(String a, String b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equalsIgnoreCase(b);
    }

    private static String safeLower(String s) {
        return s == null ? null : s.toLowerCase(Locale.ROOT);
    }

    // Simple slugify: lower, remove accents, replace non-alnum with '-', collapse dashes, trim dashes
    private static final Pattern NON_ALNUM = Pattern.compile("[^a-z0-9]+");
    private static final Pattern DASHES = Pattern.compile("-+");
    private static String slugify(String input) {
        String lower = input.toLowerCase(Locale.ROOT);
        String norm = Normalizer.normalize(lower, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        String dashed = NON_ALNUM.matcher(norm).replaceAll("-");
        String collapsed = DASHES.matcher(dashed).replaceAll("-");
        String trimmed = collapsed.replaceAll("^-+|-+$", "");
        return trimmed.isEmpty() ? "category" : trimmed;
    }
}
