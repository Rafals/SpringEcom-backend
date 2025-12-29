package org.example.springecom.service;

import org.example.springecom.model.User;
import org.example.springecom.repo.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    @Autowired
    private UserRepo userRepo;

    @Autowired
    private BCryptPasswordEncoder encoder;

    public User register(User user) {
        // HASHOWANIE HASŁA przed zapisem
        user.setPassword(encoder.encode(user.getPassword()));
        return userRepo.save(user);
    }

    public String login(String email, String password) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Porównanie hasła wpisanego z tym zhaszowanym w bazie
        if (encoder.matches(password, user.getPassword())) {
            // Tutaj wygenerujemy token JWT (krok niżej)
            return "GENERATED_JWT_TOKEN";
        }
        throw new RuntimeException("Invalid credentials");
    }

    public User getUserByEmail(String email) {
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
    }
}
