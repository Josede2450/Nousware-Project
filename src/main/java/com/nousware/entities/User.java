package com.nousware.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
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

    @Column(length = 120)
    private String lastName;

    @Column(nullable = false, unique = true, length = 190)
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
    private String provider; // "LOCAL", "GOOGLE", ...

    @Column(name = "picture_url", length = 512)
    private String pictureUrl; // from OAuth provider

    // ---- User-uploaded avatar ----
    @Column(name = "avatar_url", length = 512)
    private String avatarUrl;

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
    @JsonIgnore
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<Address> addresses = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<Project> projects = new ArrayList<>();

    // ✅ REMOVED testimonial relation (no longer needed)

    @JsonIgnore
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<BlogPost> blogPosts = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "users_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private List<Role> roles = new ArrayList<>();

    // Optional helper
    public void setRolesMutable(List<Role> newRoles) {
        if (this.roles == null) this.roles = new ArrayList<>();
        this.roles.clear();
        if (newRoles != null) this.roles.addAll(newRoles);
    }
}
