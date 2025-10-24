package com.nousware.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "testimonial")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Testimonial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int testimonialId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    private LocalDateTime createdAt;

    // ✅ Long text field to store image URL
    @Column(name = "img_url", columnDefinition = "TEXT")
    private String imgUrl;

    // ✅ Allows marking favorite testimonials
    @Column(name = "favorite")
    private boolean favorite;
}
