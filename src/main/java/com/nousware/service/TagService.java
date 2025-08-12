// src/main/java/com/nousware/service/TagService.java
package com.nousware.service;

import com.nousware.entities.BlogPost;
import com.nousware.entities.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Set;

public interface TagService {

    // Basic CRUD + search
    Page<Tag> list(Pageable pageable);
    Page<Tag> searchByName(String q, Pageable pageable);
    Tag get(int id);
    Tag getBySlug(String slug);
    Tag create(String name, String slug);
    Tag update(int id, String name, String slug);
    void delete(int id);

    // Listing posts by tag
    Page<BlogPost> listPostsByTagId(int tagId, Pageable pageable);
    Page<BlogPost> listPostsByTagSlug(String slug, Pageable pageable);

    // Relationship ops (modify from owning side = BlogPost)
    Tag attachToPosts(int tagId, Set<Integer> postIds);
    Tag detachFromPosts(int tagId, Set<Integer> postIds);
}
