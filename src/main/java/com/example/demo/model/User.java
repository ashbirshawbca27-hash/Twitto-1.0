package com.example.demo.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String username;

    private String password;

    private String mail;   // ✅ added (matches DB)

    private String role;   // ADMIN / USER

    @Column(columnDefinition = "TINYINT(1)", nullable = false)
    private boolean isOnline = false;  // Track if user is online
}