// src/main/java/com/nousware/controller/PostLikeController.java
package com.nousware.controller;

import com.nousware.dto.PostLikeCountResponse;
import com.nousware.dto.PostLikeRequest;
import com.nousware.entities.PostLike;
import com.nousware.service.PostLikeService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/post-likes")
public class PostLikeController {

    private final PostLikeService service;

    public PostLikeController(PostLikeService service) {
        this.service = service;
    }

    // POST /api/post-likes  { postId, userId }  -> returns count after like
    @PostMapping
    public ResponseEntity<PostLikeCountResponse> like(@Valid @RequestBody PostLikeRequest req) {
        long count = service.like(req.postId, req.userId);
        return ResponseEntity.ok(new PostLikeCountResponse(req.postId, count));
    }

    // DELETE /api/post-likes  { postId, userId }  -> returns count after unlike
    @DeleteMapping
    public ResponseEntity<PostLikeCountResponse> unlike(@Valid @RequestBody PostLikeRequest req) {
        long count = service.unlike(req.postId, req.userId);
        return ResponseEntity.ok(new PostLikeCountResponse(req.postId, count));
    }

    // GET /api/post-likes/count?postId=1
    @GetMapping("/count")
    public ResponseEntity<PostLikeCountResponse> count(@RequestParam int postId) {
        long count = service.countForPost(postId);
        return ResponseEntity.ok(new PostLikeCountResponse(postId, count));
    }

    // GET /api/post-likes/by-post/1?page=0&size=20
    @GetMapping("/by-post/{postId}")
    public ResponseEntity<Page<PostLike>> listByPost(
            @PathVariable int postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(service.listByPost(postId, PageRequest.of(page, size)));
    }

    // GET /api/post-likes/by-user/5?page=0&size=20
    @GetMapping("/by-user/{userId}")
    public ResponseEntity<Page<PostLike>> listByUser(
            @PathVariable int userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(service.listByUser(userId, PageRequest.of(page, size)));
    }
}
