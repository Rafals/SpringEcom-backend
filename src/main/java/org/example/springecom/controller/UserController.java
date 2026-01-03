package org.example.springecom.controller;

import org.example.springecom.model.User;
import org.example.springecom.security.JwtUtils;
import org.example.springecom.service.GoogleAuthService;
import org.example.springecom.service.RecaptchaService;
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

    @Autowired UserService userService;
    @Autowired RecaptchaService recaptchaService;
    @Autowired GoogleAuthService googleAuthService;
    @Autowired JwtUtils jwtUtils;

    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginRequest) {
        String email = loginRequest.get("email");
        String password = loginRequest.get("password");
        String captchaToken = loginRequest.get("captchaToken");

        if (captchaToken == null || !recaptchaService.verifyToken(captchaToken)) {
            return new ResponseEntity<>(Map.of("message", "Invalid Captcha"), HttpStatus.BAD_REQUEST);
        }

        try {
            User foundUser = userService.getUserByEmail(email);
            String token = userService.login(email, password);
            return ResponseEntity.ok(Map.of(
                    "token", token,
                    "username", foundUser.getUsername(),
                    "role", foundUser.getRole()
            ));
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

    @PostMapping("/auth/google")
    public ResponseEntity<?> googleLogin(@RequestBody Map<String, String> request) {
        String idToken = request.get("token");
        String jwtToken = googleAuthService.authenticateGoogleUser(idToken);

        User user = userService.getUserByEmail(jwtUtils.extractUsername(jwtToken));

        return ResponseEntity.ok(Map.of(
                "token", jwtToken,
                "username", user.getUsername(),
                "role", user.getRole()
        ));
    }

}
