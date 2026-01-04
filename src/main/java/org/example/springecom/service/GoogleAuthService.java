package org.example.springecom.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.example.springecom.model.User;
import org.example.springecom.repo.UserRepo;
import org.example.springecom.security.AuthProvider;
import org.example.springecom.security.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;

@Service
public class GoogleAuthService {

    @Autowired private UserRepo userRepo;
    @Autowired private JwtUtils jwtUtils;

    // Upewnij się, że to ID jest na 100% zgodne z frontendem
    private static final String CLIENT_ID = "735144855068-3jesaln8stpf6ino6i3mrbt0c8lk9sfq.apps.googleusercontent.com";

    public String authenticateGoogleUser(String idTokenString) {
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                .setAudience(Collections.singletonList(CLIENT_ID))
                .build();

        GoogleIdToken idToken;
        try {
            // Tylko to może rzucić błąd "Invalid Token"
            idToken = verifier.verify(idTokenString);
        } catch (Exception e) {
            throw new RuntimeException("Invalid Google Token");
        }

        if (idToken != null) {
            GoogleIdToken.Payload payload = idToken.getPayload();
            String email = payload.getEmail();

            // --- LOGIKA BIZNESOWA (POZA TRY-CATCH) ---

            User user = userRepo.findByEmail(email).orElse(null);

            // 1. Sprawdzenie bana (pełna logika zgodna ze zwykłym logowaniem)
            if (user != null && user.isBanned()) {
                // A. Sprawdź czy ban wygasł
                if (user.getBanExpiration() != null && user.getBanExpiration().isBefore(LocalDateTime.now())) {
                    // Ban minął - zdejmujemy go i pozwalamy się zalogować
                    user.setBanned(false);
                    user.setBanExpiration(null);
                    user.setBanReason(null);
                    userRepo.save(user);
                } else {
                    // B. Ban nadal aktywny - budujemy odpowiedni komunikat
                    String message = "Account suspended.";
                    if (user.getBanExpiration() != null) {
                        message += " Expires on: " + user.getBanExpiration().toString();
                    } else {
                        message += " Reason: " + (user.getBanReason() != null ? user.getBanReason() : "Permanent Ban");
                    }
                    throw new RuntimeException(message);
                }
            }

            // 2. Rejestracja nowego użytkownika
            if (user == null) {
                user = new User();
                user.setEmail(email);
                user.setUsername(email);
                user.setRole("ROLE_USER");
                user.setAuthProvider(AuthProvider.GOOGLE);
                user.setPassword("");
                userRepo.save(user);
            }

            return jwtUtils.generateToken(user.getEmail());
        } else {
            throw new RuntimeException("Invalid Google Token");
        }
    }
}