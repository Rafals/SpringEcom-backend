package org.example.springecom.controller;

import org.example.springecom.model.User;
import org.example.springecom.model.dto.BanRequest;
import org.example.springecom.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize; // Ważne!
import org.springframework.security.core.Authentication; // Do pobrania zalogowanego usera
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin
public class UserController {

    @Autowired
    private UserService userService;

    // --- LOGOWANIE ---
    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginRequest) {
        try {
            Map<String, Object> response = userService.login(
                    loginRequest.get("email"),
                    loginRequest.get("password"),
                    loginRequest.get("captchaToken")
            );
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(Map.of("message", e.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }

    // --- REJESTRACJA ---
    @PostMapping("/auth/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        try {
            User registeredUser = userService.register(user);
            return new ResponseEntity<>(registeredUser, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(Map.of("message", e.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }

    // --- GOOGLE ---
    @PostMapping("/auth/google")
    public ResponseEntity<?> googleLogin(@RequestBody Map<String, String> request) {
        try {
            Map<String, Object> response = userService.loginWithGoogle(request.get("token"));
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            // --- DIAGNOSTYKA ---
            System.out.println("--- BŁĄD LOGOWANIA GOOGLE ---");
            System.out.println("Treść błędu: " + e.getMessage());
            e.printStackTrace(); // To pokaże w konsoli, w której linii dokładnie jest błąd
            // -------------------

            String msg = e.getMessage();

            // Jeśli komunikat jest pusty, ustawiamy domyślny, żeby uniknąć błędu w logic
            if (msg == null) msg = "Unknown Error";

            if (msg.contains("suspended") || msg.contains("banned") || msg.contains("blocked")) {
                return ResponseEntity
                        .status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", msg));
            }

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", msg));
        }
    }

    // --- POBIERANIE WSZYSTKICH (DLA ADMINA) ---
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    // --- USUWANIE (ADMIN LUB SELF) ---
    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id, Authentication authentication) {
        try {
            // Wyciągamy email aktualnie zalogowanego użytkownika z tokena JWT
            String currentEmail = authentication.getName();

            userService.deleteUser(id, currentEmail);

            return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
        } catch (RuntimeException e) {
            // Jeśli brak uprawnień lub user nie istnieje
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        }
    }

    // Banowanie
    @PutMapping("/users/{id}/ban")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> banUser(@PathVariable Long id, @RequestBody BanRequest request) {
        userService.banUser(id, request.days(), request.reason());
        return ResponseEntity.ok(Map.of("message", "User banned successfully"));
    }

    // Odbanowanie
    @PutMapping("/users/{id}/unban")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> unbanUser(@PathVariable Long id) {
        userService.unbanUser(id);
        return ResponseEntity.ok(Map.of("message", "User unbanned successfully"));
    }
}