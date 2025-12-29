package org.example.springecom.service;

import org.example.springecom.model.User;
import org.example.springecom.repo.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserRepo userRepo;

    public User register(User user) {
        // Tutaj w przyszłości dodasz szyfrowanie hasła: passwordEncoder.encode(user.getPassword())
        return userRepo.save(user);
    }

    public boolean login(String email, String password) {
        return userRepo.findByEmail(email)
                .map(u -> u.getPassword().equals(password)) // Uproszczone sprawdzenie
                .orElse(false);
    }
}
