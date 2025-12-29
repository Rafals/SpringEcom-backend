package org.example.springecom.service;

import org.example.springecom.model.User;
import org.example.springecom.repo.UserRepo;
import org.example.springecom.security.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {
    @Autowired
    private UserRepo userRepo;

    @Autowired
    private BCryptPasswordEncoder encoder;

    @Autowired
    private JwtUtils jwtUtils;

    public User register(User user) {
        // HASHOWANIE HASŁA przed zapisem
        user.setPassword(encoder.encode(user.getPassword()));
        return userRepo.save(user);
    }

    public String login(String email, String password) {
        Optional<User> user = userRepo.findByEmail(email);

        if (user.isPresent() && encoder.matches(password, user.get().getPassword())) {
            // TUTAJ GENERUJEMY PRAWDZIWY TOKEN:
            return jwtUtils.generateToken(email);
        } else {
            throw new RuntimeException("Invalid credentials");
        }
    }

    public User getUserByEmail(String email) {
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
    }
}
