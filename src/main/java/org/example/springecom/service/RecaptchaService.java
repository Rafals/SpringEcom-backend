package org.example.springecom.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class RecaptchaService {

    @Value("${app.recaptcha.secret-key}")
    private String SECRET_KEY;
    private final String VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";

    public boolean verifyToken(String token) {
        RestTemplate restTemplate = new RestTemplate();

        String url = String.format("%s?secret=%s&response=%s", VERIFY_URL, SECRET_KEY, token);

        Map<String, Object> response = restTemplate.postForObject(url, null, Map.class);

        return response != null && (Boolean) response.get("success");
    }
}
