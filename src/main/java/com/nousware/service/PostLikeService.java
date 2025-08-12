// src/main/java/com/nousware/service/PostLikeService.java
package com.nousware.service;

import com.nousware.entities.PostLike;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PostLikeService {

    // Idempotent like: creates if missing, returns total count
    long like(int postId, int userId);

    // Idempotent unlike: deletes if exists, returns total count
    long unlike(int postId, int userId);

    // Queries (handy for admin/testing)
    long countForPost(int postId);
    Page<PostLike> listByPost(int postId, Pageable pageable);
    Page<PostLike> listByUser(int userId, Pageable pageable);
}
