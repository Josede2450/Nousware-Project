// src/main/java/com/nousware/dto/ViewTestimonial.java
package com.nousware.dto;

import java.time.LocalDateTime;

public class ViewTestimonial {
    public Integer id;
    public String quote;              // UI text
    public LocalDateTime createdAt;
    public boolean favorite;          // ✅ NEW

    // nested user view (only what the UI needs)
    public static class ViewUser {
        public Integer id;
        public String firstName;
        public String lastName;
        public String pictureUrl; // avatarUrl if set, else OAuth picture

        public ViewUser(Integer id, String firstName, String lastName, String pictureUrl) {
            this.id = id;
            this.firstName = firstName;
            this.lastName = lastName;
            this.pictureUrl = pictureUrl;
        }
    }

    public ViewUser user;

    public ViewTestimonial(Integer id, String quote, LocalDateTime createdAt, ViewUser user, boolean favorite) {
        this.id = id;
        this.quote = quote;
        this.createdAt = createdAt;
        this.user = user;
        this.favorite = favorite;
    }
}
