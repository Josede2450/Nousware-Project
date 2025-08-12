// src/main/java/com/nousware/repository/PostLikeRepository.java
package com.nousware.repository;

import com.nousware.entities.BlogPost;
import com.nousware.entities.PostLike;
import com.nousware.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    boolean existsByBlogPostAndUser(BlogPost post, User user);

    Optional<PostLike> findByBlogPostAndUser(BlogPost post, User user);

    long countByBlogPost(BlogPost post);

    Page<PostLike> findByBlogPost_PostId(int postId, Pageable pageable);

    Page<PostLike> findByUser_UserId(int userId, Pageable pageable);
}
