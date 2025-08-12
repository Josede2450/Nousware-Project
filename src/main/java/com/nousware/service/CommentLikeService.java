// src/main/java/com/nousware/service/CommentLikeService.java
package com.nousware.service;

import com.nousware.entities.CommentLike;
import org.springframework.data.domain.*;

public interface CommentLikeService {
    long like(int commentId, int userId);
    long unlike(int commentId, int userId);
    long countForComment(int commentId);
    Page<CommentLike> listByComment(int commentId, Pageable pageable);
    Page<CommentLike> listByUser(int userId, Pageable pageable);
}
