// src/main/java/com/nousware/controller/TagController.java
package com.nousware.controller;

import com.nousware.dto.TagCreateRequest;
import com.nousware.dto.TagUpdateRequest;
import com.nousware.entities.BlogPost;
import com.nousware.entities.Tag;
import com.nousware.service.TagService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Set;

@RestController
@RequestMapping("/api/tags")
public class TagController {

    private final TagService service;

    public TagController(TagService service) {
        this.service = service;
    }

    // GET /api/tags?page=0&size=20&search=java
    @GetMapping
    public ResponseEntity<Page<Tag>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Tag> result = (search == null || search.isBlank())
                ? service.list(pageable)
                : service.searchByName(search, pageable);
        return ResponseEntity.ok(result);
    }

    // GET /api/tags/{id}
    @GetMapping("/{id}")
    public ResponseEntity<Tag> get(@PathVariable int id) {
        return ResponseEntity.ok(service.get(id));
    }

    // GET /api/tags/slug/{slug}
    @GetMapping("/slug/{slug}")
    public ResponseEntity<Tag> getBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(service.getBySlug(slug));
    }

    // POST /api/tags
    @PostMapping
    public ResponseEntity<Tag> create(@Valid @RequestBody TagCreateRequest req) {
        Tag created = service.create(req.name, req.slug);
        return ResponseEntity.created(URI.create("/api/tags/" + created.getTagId())).body(created);
    }

    // PUT /api/tags/{id}
    @PutMapping("/{id}")
    public ResponseEntity<Tag> update(@PathVariable int id, @RequestBody TagUpdateRequest req) {
        Tag updated = service.update(id, req.name, req.slug);
        return ResponseEntity.ok(updated);
    }

    // DELETE /api/tags/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable int id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    // GET /api/tags/{id}/posts
    @GetMapping("/{id}/posts")
    public ResponseEntity<Page<BlogPost>> listPostsByTagId(
            @PathVariable int id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(service.listPostsByTagId(id, PageRequest.of(page, size)));
    }

    // GET /api/tags/slug/{slug}/posts
    @GetMapping("/slug/{slug}/posts")
    public ResponseEntity<Page<BlogPost>> listPostsByTagSlug(
            @PathVariable String slug,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(service.listPostsByTagSlug(slug, PageRequest.of(page, size)));
    }

    // POST /api/tags/{id}/attach
    @PostMapping("/{id}/attach")
    public ResponseEntity<Tag> attachToPosts(@PathVariable int id, @RequestBody Set<Integer> postIds) {
        return ResponseEntity.ok(service.attachToPosts(id, postIds));
    }

    // POST /api/tags/{id}/detach
    @PostMapping("/{id}/detach")
    public ResponseEntity<Tag> detachFromPosts(@PathVariable int id, @RequestBody Set<Integer> postIds) {
        return ResponseEntity.ok(service.detachFromPosts(id, postIds));
    }
}
