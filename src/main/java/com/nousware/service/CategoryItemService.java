package com.nousware.service;

import com.nousware.entities.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CategoryItemService {
    Category create(Category input);
    Category update(Integer id, Category input);
    void delete(Integer id);
    Category get(Integer id);

    /**
     * List categories. If q is provided, filters by name/slug (case-insensitive).
     * Uses in-memory filtering if your repository doesn't have a search method.
     */
    Page<Category> list(String q, Pageable pageable);
}
