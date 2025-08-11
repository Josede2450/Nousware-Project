package com.nousware.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(
        name = "users",
        indexes = {
                @Index(name = "idx_users_email", columnList = "email"),
                @Index(name = "idx_users_google_sub", columnList = "google_sub")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int userId;

    @Column(length = 120)
    private String name;

    @Column(nullable = false, unique = true, length = 190) // 190 safe for utf8mb4 + index
    private String email;

    @Column(length = 20)
    private String gender;

    private boolean enable;

    @Column(length = 30)
    private String phone;

    @JsonIgnore
    private String password;

    // ---- OAuth fields ----
    @Column(name = "google_sub", unique = true, length = 64)
    private String googleSub;

    @Column(length = 20)
    private String provider; // e.g., "LOCAL", "GOOGLE"

    @Column(name = "picture_url", length = 512)
    private String pictureUrl;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    // ---- Timestamps ----
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ---- Relations ----
    @OneToMany(mappedBy = "user")
    private List<Address> addresses;

    @OneToMany(mappedBy = "user")
    private List<Project> projects;

    @OneToMany(mappedBy = "user")
    private List<Testimonial> testimonials;

    @OneToMany(mappedBy = "user")
    private List<BlogPost> blogPosts;

    @ManyToMany
    @JoinTable(
            name = "users_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private List<Role> roles;
}
