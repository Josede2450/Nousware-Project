package com.nousware.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "service")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Service {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "service_id")
    @EqualsAndHashCode.Include
    private Integer serviceId;

    @Column(name = "title")
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "summary")
    private String summary;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "most_popular")
    private boolean mostPopular;

    // NEW
    @Column(name = "price_range", length = 250)   // VARCHAR(250)
    private String priceRange;

    // NEW
    @Column(name = "duration", length = 255)      // VARCHAR(255)
    private String duration;

    @ManyToMany
    @JoinTable(
            name = "service_category",
            joinColumns = @JoinColumn(name = "service_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private Set<Category> categories = new HashSet<>();
}
