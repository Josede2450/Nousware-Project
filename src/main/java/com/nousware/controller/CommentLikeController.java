// src/main/java/com/nousware/controller/CommentLikeController.java
package com.nousware.controller;

import com.nousware.entities.CommentLike;
import com.nousware.service.CommentLikeService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/comment-likes")
public class CommentLikeController {

    private final CommentLikeService service;

    public CommentLikeController(CommentLikeService service) { this.service = service; }

    // Like comment
    @PostMapping
    public ResponseEntity<Long> like(@RequestParam int commentId, @RequestParam int userId) {
        return ResponseEntity.ok(service.like(commentId, userId));
    }

    // Unlike comment
    @DeleteMapping
    public ResponseEntity<Long> unlike(@RequestParam int commentId, @RequestParam int userId) {
        return ResponseEntity.ok(service.unlike(commentId, userId));
    }

    // Count
    @GetMapping("/count")
    public ResponseEntity<Long> count(@RequestParam int commentId) {
        return ResponseEntity.ok(service.countForComment(commentId));
    }

    // Lists (handy for testing)
    @GetMapping("/by-comment/{commentId}")
    public ResponseEntity<Page<CommentLike>> listByComment(
            @PathVariable int commentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(service.listByComment(commentId, PageRequest.of(page, size)));
    }

    @GetMapping("/by-user/{userId}")
    public ResponseEntity<Page<CommentLike>> listByUser(
            @PathVariable int userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(service.listByUser(userId, PageRequest.of(page, size)));
    }
}
