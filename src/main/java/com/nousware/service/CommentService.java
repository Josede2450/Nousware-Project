// src/main/java/com/nousware/service/CommentService.java
package com.nousware.service;

import com.nousware.entities.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CommentService {

    Comment create(String content, int postId, int userId, Integer parentCommentId);

    Comment update(int commentId, String content);

    void delete(int commentId);

    Page<Comment> listByPost(int postId, Pageable pageable);

    Page<Comment> listTopLevelByPost(int postId, Pageable pageable);

    Page<Comment> listByUser(int userId, Pageable pageable);
}
