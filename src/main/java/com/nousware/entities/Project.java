package com.nousware.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "project")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int projectId;

    private String goals;
    private double budget;
    private LocalDateTime deadline;
    private String preferences;
    private String competitorReferences;
    private String status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    // Derived fields (not stored in DB)
    @Transient
    public String getFirstName() {
        return user != null ? user.getName().split(" ")[0] : null;
    }

    @Transient
    public String getLastName() {
        if (user != null && user.getName().contains(" ")) {
            return user.getName().substring(user.getName().indexOf(" ") + 1);
        }
        return null;
    }

    @Transient
    public String getPhone() {
        return user != null ? user.getPhone() : null;
    }
}
