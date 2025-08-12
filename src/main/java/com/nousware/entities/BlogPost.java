package com.nousware.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "blog_post")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BlogPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int postId;

    private String title;
    @Column(columnDefinition = "TEXT")
    private String content;

    // REMOVE likeCount column from DB (see migration below)
    private String slug;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @OneToMany(mappedBy = "blogPost", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private Set<Comment> comments;

    @ManyToMany
    @JoinTable(
            name = "post_tag",
            joinColumns = @JoinColumn(name = "post_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> tags;

    // New: likes (post <-> user)
    @OneToMany(mappedBy = "blogPost", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private Set<PostLike> likes;

    @Transient
    public int getLikeCount() {
        return likes == null ? 0 : likes.size();
    }
}
