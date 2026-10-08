package com.investment.Group.management.model;


import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    /**
     * Stored as a BCrypt hash, never in plain text.
     *
     * {@link JsonIgnore} is deliberately placed on the field so that the
     * property is excluded from EVERY serialization path. Relying on a DTO
     * alone is fragile: any controller that returns an entity directly, now
     * or in future, would silently start leaking credentials again. Keeping
     * the guard on the entity makes leaking impossible by construction.
     */
    @JsonIgnore
    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String role; // ADMIN or MEMBER

    private LocalDateTime createdAt = LocalDateTime.now();

    // Constructors
    public User() {}

    public User(String username, String email, String password, String role) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.role = role;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Annotated with {@link JsonIgnore} as well as the field, so the password
     * cannot leak through the getter even if the field annotation is ever
     * refactored away.
     */
    @JsonIgnore
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
