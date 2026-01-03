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

import java.util.Collections;

@Service
public class GoogleAuthService {

    @Autowired private UserRepo userRepo;
    @Autowired private JwtUtils jwtUtils;

    private static final String CLIENT_ID = "735144855068-3jesaln8stpf6ino6i3mrbt0c8lk9sfq.apps.googleusercontent.com";

    public String authenticateGoogleUser(String idTokenString) {
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                .setAudience(Collections.singletonList(CLIENT_ID))
                .build();

        try {
            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken != null) {
                GoogleIdToken.Payload payload = idToken.getPayload();
                String email = payload.getEmail();
                String name = (String) payload.get("name"); // Imię i nazwisko z Google

                User user = userRepo.findByEmail(email).orElse(null);

                if (user == null) {
                    // SCENARIUSZ: Nowy użytkownik -> Rejestrujemy go automatycznie!
                    user = new User();
                    user.setEmail(email);
                    user.setUsername(email); // Lub wyciągnij name
                    user.setRole("ROLE_USER");
                    user.setAuthProvider(AuthProvider.GOOGLE);
                    user.setPassword(""); // Brak hasła
                    userRepo.save(user);
                }

                // Generujemy NASZ token JWT (ten sam, którego używa reszta apki)
                return jwtUtils.generateToken(user.getEmail());
            }
        } catch (Exception e) {
            throw new RuntimeException("Invalid Google Token");
        }
        return null;
    }
}