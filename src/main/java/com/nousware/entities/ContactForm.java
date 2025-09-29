package com.nousware.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "contact_form")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContactForm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int contactId;

    private String name;
    private String email;
    private String phone;
    private String message;

    private LocalDateTime createdAt;

    // Convenience getter so you can call getId() in logs, etc.
    public Integer getId() {
        return contactId;
    }
}
