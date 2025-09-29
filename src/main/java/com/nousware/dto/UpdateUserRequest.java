package com.nousware.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Fields that admins (or the user, depending on your security rules) can edit.
 * All are optional; only non-null values will be applied.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateUserRequest {

    /** New email (normalized lower-case). Null = keep current. */
    @Email(message = "Invalid email")
    public String email;

    /** First name stored in `User.name`. Null = keep current. */
    @Size(max = 100, message = "First name too long")
    public String firstName;

    /** Last name stored in `User.lastName`. Null = keep current. */
    @Size(max = 100, message = "Last name too long")
    public String lastName;

    /** Optional phone. Null = keep current. */
    @Size(max = 50, message = "Phone too long")
    public String phone;

    /** Optional gender. Null = keep current. */
    @Size(max = 50, message = "Gender too long")
    public String gender;

    /**
     * Enable / disable account. Null = keep current.
     * Accepts "enabled", "enable", or "isEnabled" as JSON keys.
     */
    @JsonAlias({"enable", "isEnabled"})
    public Boolean enabled;

    /**
     * Replace roles with these role names exactly (e.g., ["ADMIN","CLIENT"]).
     * Null = do not change roles. Empty list = clear roles.
     */
    public List<String> roles;

    /**
     * Optional avatar URL. To clear avatar, send an empty string "".
     * Null = don't change avatar.
     */
    public String avatarUrl;
}
