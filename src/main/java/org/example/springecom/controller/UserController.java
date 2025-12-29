package org.example.springecom.controller;

import org.example.springecom.model.User;
import org.example.springecom.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin
public class UserController {

    @Autowired
    UserService userService;

    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@RequestBody User user) {
        try {
            // Pobieramy pełne dane użytkownika z bazy, aby znać jego rolę i username
            User foundUser = userService.getUserByEmail(user.getEmail());
            String token = userService.login(user.getEmail(), user.getPassword());

            System.out.println("Login success: " + foundUser.getUsername());

            // Zwracamy obiekt z tokenem, nazwą i rolą
            return new ResponseEntity<>(Map.of(
                    "token", token,
                    "username", foundUser.getUsername(),
                    "role", foundUser.getRole() // np. "ROLE_ADMIN" lub "ROLE_USER"
            ), HttpStatus.OK);

        } catch (RuntimeException e) {
            return new ResponseEntity<>(Map.of("message", "Invalid credentials"), HttpStatus.UNAUTHORIZED);
        }
    }

    @PostMapping("/auth/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        try {
            if (user.getRole() == null) {
                user.setRole("ROLE_USER");
            }

            User registeredUser = userService.register(user);
            System.out.println("Register success: " + user.getUsername());
            return new ResponseEntity<>(registeredUser, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(Map.of("message", "Registration failed: " + e.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }

}
