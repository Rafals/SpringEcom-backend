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
import java.util.Random;

@Service
public class UserService {

    @Autowired private UserRepo userRepo;
    @Autowired private BCryptPasswordEncoder encoder;
    @Autowired private JwtUtils jwtUtils;
    @Autowired private RecaptchaService recaptchaService;
    @Autowired private GoogleAuthService googleAuthService;
    @Autowired private EmailService emailService;

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

        // Ustawienia domyślne
        if (user.getRole() == null || user.getRole().isEmpty()) {
            user.setRole("ROLE_USER");
        }
        user.setPassword(encoder.encode(user.getPassword()));

        // NOWOŚĆ: Blokada konta i generowanie kodu
        user.setEnabled(false);
        String verificationCode = String.format("%06d", new Random().nextInt(999999)); // Generuje np. "048123"
        user.setVerificationCode(verificationCode);

        User savedUser = userRepo.save(user);

        // Wysyłka maila (robimy to po zapisie, żeby mieć pewność, że user jest w bazie)
        emailService.sendVerificationMail(user.getEmail(), verificationCode);

        return savedUser;
    }

    public Map<String, Object> login(String email, String password, String captchaToken) {
        if (captchaToken == null || !recaptchaService.verifyToken(captchaToken)) {
            throw new RuntimeException("Invalid Captcha");
        }
        User user = getUserByEmail(email);

        checkBanStatus(user);

        if (!user.isEnabled()) {
            throw new RuntimeException("Account not verified. Please check your email.");
        }

        if (!encoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }
        return generateAuthResponse(user);
    }

    public void changePassword(String email, String oldPassword, String newPassword) {
        User user = getUserByEmail(email);

        // Sprawdzamy, czy stare hasło pasuje
        if (!encoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("Invalid old password");
        }

        user.setPassword(encoder.encode(newPassword));
        userRepo.save(user);

        // Wysyłamy powiadomienie (dla bezpieczeństwa)
        emailService.sendSecurityAlert(user.getEmail(), "Password Changed", "Twoje hasło zostało zmienione pomyślnie.");
    }

    public void requestEmailChange(String currentEmail, String newEmail) {
        User user = getUserByEmail(currentEmail);

        if (userRepo.findByEmail(newEmail).isPresent()) {
            throw new RuntimeException("This email is already in use");
        }

        String code = String.format("%06d", new java.util.Random().nextInt(999999));
        user.setVerificationCode(code);
        user.setPendingEmail(newEmail); // Zapisujemy, na co chce zmienić
        userRepo.save(user);

        // --- ZMIANA TUTAJ ---
        // Było: emailService.sendVerificationEmail(newEmail, code);
        // Jest: Wysyłamy na OBECNY mail (currentEmail), żeby właściciel potwierdził
        emailService.sendEmailChangeConfirmation(currentEmail, code);
    }

    public Map<String, Object> confirmEmailChange(String currentEmail, String code) {
        User user = getUserByEmail(currentEmail);

        if (user.getVerificationCode() != null && user.getVerificationCode().equals(code)) {
            String newEmail = user.getPendingEmail();

            user.setEmail(newEmail); // Podmieniamy maila
            user.setPendingEmail(null);
            user.setVerificationCode(null);
            userRepo.save(user);

            // Musimy wygenerować NOWY token, bo stary zawierał stary email!
            return generateAuthResponse(user);
        } else {
            throw new RuntimeException("Invalid verification code");
        }
    }

    public void verifyUser(String email, String code) {
        User user = getUserByEmail(email); // To rzuci błąd jak user nie istnieje

        if (user.isEnabled()) {
            throw new RuntimeException("Account already verified");
        }

        if (user.getVerificationCode() != null && user.getVerificationCode().equals(code)) {
            user.setEnabled(true);
            user.setVerificationCode(null); // Czyścimy kod po użyciu
            userRepo.save(user);
        } else {
            throw new RuntimeException("Invalid verification code");
        }
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


    // --- RESET HASŁA: KROK 1 (Generowanie kodu) ---
    public void forgotPassword(String email) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found")); // Dla bezpieczeństwa można nie mówić wprost, ale na devie zostawmy tak

        // Generujemy kod 6 cyfr
        String code = String.format("%06d", new java.util.Random().nextInt(999999));

        user.setResetToken(code);
        user.setResetTokenExpiration(LocalDateTime.now().plusMinutes(15)); // Ważny 15 min
        userRepo.save(user);

        emailService.sendPasswordResetEmail(user.getEmail(), code);
    }

    // --- RESET HASŁA: KROK 2 (Zmiana hasła) ---
    public void resetPassword(String email, String code, String newPassword) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getResetToken() == null || !user.getResetToken().equals(code)) {
            throw new RuntimeException("Invalid reset code");
        }

        if (user.getResetTokenExpiration().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Reset code expired");
        }

        // Wszystko ok - zmieniamy hasło
        user.setPassword(encoder.encode(newPassword));

        // Czyścimy token
        user.setResetToken(null);
        user.setResetTokenExpiration(null);

        userRepo.save(user);
    }
}