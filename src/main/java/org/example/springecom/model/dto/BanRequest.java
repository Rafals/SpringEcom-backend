package org.example.springecom.model.dto;

public record BanRequest(
        int days,       // 0 lub null oznacza ban permanentny
        String reason   // np. "Spamowanie"
) {}
