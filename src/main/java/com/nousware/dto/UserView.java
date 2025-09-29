package com.nousware.dto;

import java.time.LocalDateTime;
import java.util.List;

/** Read-only projection you return to clients (keeps password internal). */
public class UserView {
    /** Unique database id (primary key). */
    public Integer id;
    /** First name stored in `User.name`. */
    public String firstName;
    /** Last name stored in `User.lastName`. */
    public String lastName;
    /** Email (normalized lower-case). */
    public String email;
    /** Whether account is enabled. */
    public boolean enabled;
    /** Auth provider e.g. LOCAL / GOOGLE. */
    public String provider;
    /** Preferred display picture (avatar > pictureUrl > default). */
    public String displayPictureUrl;
    /** Direct avatar URL (user-uploaded), may be null. */
    public String avatarUrl;
    /** OAuth picture URL (e.g., Google), may be null. */
    public String pictureUrl;
    /** Optional phone. */
    public String phone;
    /** Optional gender. */
    public String gender;
    /** Role names (e.g., ["ADMIN","CLIENT"]). */
    public List<String> roles;
    /** Timestamps for auditing. */
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
    public LocalDateTime lastLoginAt;
}
