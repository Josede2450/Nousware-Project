// src/main/java/com/nousware/controller/BlogPostController.java
package com.nousware.controller;

import com.nousware.dto.BlogPostCreateRequest;
import com.nousware.dto.BlogPostUpdateRequest;
import com.nousware.entities.BlogPost;
import com.nousware.service.BlogPostService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Set;

@RestController
@RequestMapping("/api/posts")
public class BlogPostController {

    private final BlogPostService service;

    public BlogPostController(BlogPostService service) {
        this.service = service;
    }

    // GET /api/posts?page=0&size=20&search=foo
    @GetMapping
    public ResponseEntity<Page<BlogPost>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<BlogPost> result = (search == null || search.isBlank())
                ? service.list(pageable)
                : service.searchByTitle(search, pageable);
        return ResponseEntity.ok(result);
    }

    // GET /api/posts/{id}
    @GetMapping("/{id}")
    public ResponseEntity<BlogPost> get(@PathVariable int id) {
        return ResponseEntity.ok(service.get(id));
    }

    // GET /api/posts/slug/{slug}
    @GetMapping("/slug/{slug}")
    public ResponseEntity<BlogPost> getBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(service.getBySlug(slug));
    }

    // POST /api/posts
    @PostMapping
    public ResponseEntity<BlogPost> create(@Valid @RequestBody BlogPostCreateRequest req) {
        BlogPost created = service.create(req.title, req.content, req.slug, req.userId, req.tagIds);
        return ResponseEntity.created(URI.create("/api/posts/" + created.getPostId())).body(created);
    }

    // PUT /api/posts/{id}
    @PutMapping("/{id}")
    public ResponseEntity<BlogPost> update(@PathVariable int id, @RequestBody BlogPostUpdateRequest req) {
        BlogPost updated = service.update(id, req.title, req.content, req.slug, req.tagIds);
        return ResponseEntity.ok(updated);
    }

    // DELETE /api/posts/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable int id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    // POST /api/posts/{id}/tags
    @PostMapping("/{id}/tags")
    public ResponseEntity<BlogPost> addTags(@PathVariable int id, @RequestBody Set<Integer> tagIds) {
        return ResponseEntity.ok(service.addTags(id, tagIds));
    }

    // DELETE /api/posts/{id}/tags
    @DeleteMapping("/{id}/tags")
    public ResponseEntity<BlogPost> removeTags(@PathVariable int id, @RequestBody Set<Integer> tagIds) {
        return ResponseEntity.ok(service.removeTags(id, tagIds));
    }

    // POST /api/posts/{id}/like?userId=123
    @PostMapping("/{id}/like")
    public ResponseEntity<Integer> like(@PathVariable int id, @RequestParam int userId) {
        return ResponseEntity.ok(service.like(id, userId));
    }

    // POST /api/posts/{id}/unlike?userId=123
    @PostMapping("/{id}/unlike")
    public ResponseEntity<Integer> unlike(@PathVariable int id, @RequestParam int userId) {
        return ResponseEntity.ok(service.unlike(id, userId));
    }
}
