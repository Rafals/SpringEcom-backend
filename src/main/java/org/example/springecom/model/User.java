package org.example.springecom.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users") // <--- Zmień nazwę tabeli na "users"
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Sugestia: dodaj automatyczne generowanie ID
    private long id;

    private String username;
    private String password;
    private String email;
    private String role;
}
