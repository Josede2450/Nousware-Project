// src/main/java/com/nousware/controller/CommentController.java
package com.nousware.controller;

import com.nousware.dto.CommentCreateRequest;
import com.nousware.dto.CommentUpdateRequest;
import com.nousware.entities.Comment;
import com.nousware.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/comments")
public class CommentController {

    private final CommentService service;

    public CommentController(CommentService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Comment> create(@Valid @RequestBody CommentCreateRequest req) {
        Comment created = service.create(req.content, req.postId, req.userId, req.parentCommentId);
        return ResponseEntity.created(URI.create("/api/comments/" + created.getCommentId())).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Comment> update(@PathVariable int id, @Valid @RequestBody CommentUpdateRequest req) {
        return ResponseEntity.ok(service.update(id, req.content));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable int id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/by-post/{postId}")
    public ResponseEntity<Page<Comment>> listByPost(
            @PathVariable int postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(service.listByPost(postId, PageRequest.of(page, size)));
    }

    @GetMapping("/by-post/{postId}/top-level")
    public ResponseEntity<Page<Comment>> listTopLevelByPost(
            @PathVariable int postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(service.listTopLevelByPost(postId, PageRequest.of(page, size)));
    }

    @GetMapping("/by-user/{userId}")
    public ResponseEntity<Page<Comment>> listByUser(
            @PathVariable int userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(service.listByUser(userId, PageRequest.of(page, size)));
    }
}
