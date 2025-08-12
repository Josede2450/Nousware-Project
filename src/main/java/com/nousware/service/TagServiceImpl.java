// src/main/java/com/nousware/service/impl/TagServiceImpl.java
package com.nousware.service.impl;

import com.nousware.entities.BlogPost;
import com.nousware.entities.Tag;
import com.nousware.repository.BlogPostRepository;
import com.nousware.repository.TagRepository;
import com.nousware.service.TagService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
@Transactional
public class TagServiceImpl implements TagService {

    private final TagRepository tagRepo;
    private final BlogPostRepository postRepo;

    public TagServiceImpl(TagRepository tagRepo, BlogPostRepository postRepo) {
        this.tagRepo = tagRepo;
        this.postRepo = postRepo;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Tag> list(Pageable pageable) {
        return tagRepo.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Tag> searchByName(String q, Pageable pageable) {
        return tagRepo.findByNameContainingIgnoreCase(q == null ? "" : q, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Tag get(int id) {
        return tagRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Tag " + id + " not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public Tag getBySlug(String slug) {
        return tagRepo.findBySlug(slug)
                .orElseThrow(() -> new EntityNotFoundException("Tag with slug '" + slug + "' not found"));
    }

    @Override
    public Tag create(String name, String slug) {
        if (tagRepo.existsBySlug(slug)) {
            throw new IllegalArgumentException("Slug already in use: " + slug);
        }
        Tag tag = new Tag();
        tag.setName(name);
        tag.setSlug(slug);
        // blogPosts left null/empty; associations are managed from BlogPost side
        return tagRepo.save(tag);
    }

    @Override
    public Tag update(int id, String name, String slug) {
        Tag tag = get(id);

        if (slug != null && !slug.equals(tag.getSlug())) {
            if (tagRepo.existsBySlug(slug)) {
                throw new IllegalArgumentException("Slug already in use: " + slug);
            }
            tag.setSlug(slug);
        }
        if (name != null) tag.setName(name);

        return tagRepo.save(tag);
    }

    @Override
    public void delete(int id) {
        Tag tag = get(id);
        // Remove association from the owning side to avoid stale join rows
        if (tag.getBlogPosts() != null) {
            // Copy to avoid ConcurrentModification
            Set<BlogPost> posts = new HashSet<>(tag.getBlogPosts());
            for (BlogPost p : posts) {
                if (p.getTags() != null) {
                    p.getTags().removeIf(t -> t.getTagId() == id);
                    postRepo.save(p);
                }
            }
        }
        tagRepo.delete(tag);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BlogPost> listPostsByTagId(int tagId, Pageable pageable) {
        return postRepo.findByTags_TagId(tagId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BlogPost> listPostsByTagSlug(String slug, Pageable pageable) {
        return postRepo.findByTags_Slug(slug, pageable);
    }

    @Override
    public Tag attachToPosts(int tagId, Set<Integer> postIds) {
        if (postIds == null || postIds.isEmpty()) return get(tagId);
        Tag tag = get(tagId);

        postRepo.findAllById(postIds).forEach(post -> {
            if (post.getTags() == null) post.setTags(new HashSet<>());
            post.getTags().add(tag);
            postRepo.save(post); // owning side persists the join row
        });

        // Refresh and return
        return get(tagId);
    }

    @Override
    public Tag detachFromPosts(int tagId, Set<Integer> postIds) {
        if (postIds == null || postIds.isEmpty()) return get(tagId);
        Tag tag = get(tagId);

        postRepo.findAllById(postIds).forEach(post -> {
            if (post.getTags() != null) {
                post.getTags().removeIf(t -> t.getTagId() == tagId);
                postRepo.save(post);
            }
        });

        return get(tagId);
    }
}
