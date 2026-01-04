package org.example.springecom.service;

import org.example.springecom.model.User;
import org.example.springecom.repo.UserRepo;
import org.example.springecom.security.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Service
public class UserService {

    @Autowired private UserRepo userRepo;
    @Autowired private BCryptPasswordEncoder encoder;
    @Autowired private JwtUtils jwtUtils;
    @Autowired private RecaptchaService recaptchaService;
    @Autowired private GoogleAuthService googleAuthService;

    private void checkBanStatus(User user) {
        if (user.isBanned()) {
            if (user.getBanExpiration() != null && user.getBanExpiration().isBefore(LocalDateTime.now())) {
                user.setBanned(false);
                user.setBanExpiration(null);
                user.setBanReason(null);
                userRepo.save(user);
            } else {
                String message = "Account suspended.";
                if (user.getBanExpiration() != null) {
                    message += " Expires on: " + user.getBanExpiration().toString();
                } else {
                    message += " Reason: " + (user.getBanReason() != null ? user.getBanReason() : "Permanent Ban");
                }
                throw new RuntimeException(message);
            }
        }
    }

    public User register(User user) {
        if (userRepo.findByEmail(user.getEmail()).isPresent()) {
            throw new RuntimeException("Email already taken");
        }
        if (user.getRole() == null || user.getRole().isEmpty()) {
            user.setRole("ROLE_USER");
        }
        user.setPassword(encoder.encode(user.getPassword()));
        return userRepo.save(user);
    }

    public Map<String, Object> login(String email, String password, String captchaToken) {
        if (captchaToken == null || !recaptchaService.verifyToken(captchaToken)) {
            throw new RuntimeException("Invalid Captcha");
        }
        User user = getUserByEmail(email);

        checkBanStatus(user);

        if (!encoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }
        return generateAuthResponse(user);
    }

    public Map<String, Object> loginWithGoogle(String idToken) {
        String jwtToken = googleAuthService.authenticateGoogleUser(idToken);
        String email = jwtUtils.extractUsername(jwtToken);
        User user = getUserByEmail(email);

        checkBanStatus(user);

        return Map.of("token", jwtToken, "username", user.getUsername(), "role", user.getRole());
    }

    public void banUser(Long userId, int days, String reason) {
        User user = userRepo.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

        if ("ROLE_ADMIN".equals(user.getRole())) {
            throw new RuntimeException("Cannot ban an Admin");
        }

        user.setBanned(true);
        user.setBanReason(reason);

        if (days > 0) {
            user.setBanExpiration(LocalDateTime.now().plusDays(days));
        } else {
            user.setBanExpiration(null);
        }
        userRepo.save(user);
    }

    public void unbanUser(Long userId) {
        User user = userRepo.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        user.setBanned(false);
        user.setBanExpiration(null);
        user.setBanReason(null);
        userRepo.save(user);
    }

    private Map<String, Object> generateAuthResponse(User user) {
        String token = jwtUtils.generateToken(user.getEmail());
        return Map.of("token", token, "username", user.getUsername(), "role", user.getRole());
    }

    @Transactional
    public void deleteUser(Long targetId, String requesterEmail) {
        User requester = getUserByEmail(requesterEmail);

        User targetUser = userRepo.findById(targetId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean isAdmin = "ROLE_ADMIN".equals(requester.getRole());
        boolean isSelfDelete = requester.getId().equals(targetUser.getId());

        if (isAdmin || isSelfDelete) {
            userRepo.delete(targetUser);
        } else {
            throw new RuntimeException("Access Denied: You can only delete your own account");
        }
    }

    public User getUserByEmail(String email) {
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
    }

    public java.util.List<User> getAllUsers() {
        return userRepo.findAll();
    }
}