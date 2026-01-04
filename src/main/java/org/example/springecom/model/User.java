package org.example.springecom.model;

import jakarta.persistence.*;
import lombok.Data;
import org.example.springecom.security.AuthProvider;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "is_enabled")
    private boolean isEnabled = false;

    @Column(name = "verification_code")
    private String verificationCode;

    @Column(name = "pending_email")
    private String pendingEmail;

    @Column(name = "reset_token")
    private String resetToken;

    @Column(name = "reset_token_expiration")
    private LocalDateTime resetTokenExpiration;

    private String username;
    private String email;
    private String password;
    private String role;

    @Enumerated(EnumType.STRING)
    private AuthProvider authProvider;

    // --- NOWE POLA DO BANOWANIA ---
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean isBanned = false; // Czy zablokowany
    private LocalDateTime banExpiration; // Do kiedy (null = na zawsze)
    private String banReason; // Dlaczego
}