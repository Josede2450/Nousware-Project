package com.nousware.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "faq")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Faq {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int faqId;

    @Column(nullable = false, length = 500) // or bigger if you want a VARCHAR
    private String question;

    @Lob // Large Object (maps to TEXT in MySQL)
    @Column(columnDefinition = "TEXT")
    private String answer;
}
