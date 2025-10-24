// src/main/java/com/nousware/dto/ViewTestimonial.java
package com.nousware.dto;

import java.time.LocalDateTime;

public class ViewTestimonial {
    public Integer id;
    public String quote;              // testimonial text
    public LocalDateTime createdAt;
    public boolean favorite;          // user-favorite marker
    public String imgUrl;             // ✅ new image URL field

    // (User relation removed — keep structure for backward compatibility)
    public ViewUser user;

    // Optional nested class for compatibility — may be null now
    public static class ViewUser {
        public Integer id;
        public String firstName;
        public String lastName;
        public String pictureUrl;

        public ViewUser(Integer id, String firstName, String lastName, String pictureUrl) {
            this.id = id;
            this.firstName = firstName;
            this.lastName = lastName;
            this.pictureUrl = pictureUrl;
        }
    }

    public ViewTestimonial(Integer id, String quote, LocalDateTime createdAt, boolean favorite, String imgUrl) {
        this.id = id;
        this.quote = quote;
        this.createdAt = createdAt;
        this.favorite = favorite;
        this.imgUrl = imgUrl;
        this.user = null; // no user in new model
    }
}
