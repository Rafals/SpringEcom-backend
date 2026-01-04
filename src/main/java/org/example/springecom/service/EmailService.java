package org.example.springecom.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendVerificationMail(String toEmail, String code) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Potwierdzenie rejestracji SpringEcom");

            // Link kieruje na frontend, przekazując email w parametrze URL
            String verifyLink = "http://localhost:5173/verify?email=" + toEmail;

            message.setText("Witaj!\n\n" +
                    "Twoje konto zostało utworzone.\n" +
                    "Twój kod weryfikacyjny to: " + code + "\n\n" +
                    "Kliknij tutaj, aby wpisać kod: " + verifyLink + "\n\n" +
                    "Jeśli link nie działa, skopiuj kod i wpisz go ręcznie.");

            mailSender.send(message);
            System.out.println("Mail wysłany do: " + toEmail);
        } catch (Exception e) {
            System.err.println("Błąd wysyłania maila: " + e.getMessage());
        }
    }

    public void sendSecurityAlert(String toEmail, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Security Alert: " + subject);
            message.setText(text + "\n\nJeśli to nie Ty, skontaktuj się z nami natychmiast.");
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Błąd maila: " + e.getMessage());
        }
    }

    public void sendEmailChangeConfirmation(String toEmail, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Potwierdzenie zmiany adresu email");
        message.setText("Otrzymaliśmy prośbę o zmianę adresu email dla Twojego konta.\n\n" +
                "Twój kod weryfikacyjny to: " + code + "\n\n" +
                "Jeśli to nie Ty zleciłeś zmianę, zignoruj tę wiadomość – Twoje konto jest bezpieczne.");
        mailSender.send(message);
    }

    public void sendPasswordResetEmail(String toEmail, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Reset Hasła - SpringEcom");
        message.setText("Otrzymaliśmy prośbę o reset hasła.\n\n" +
                "Twój kod to: " + code + "\n\n" +
                "Kod jest ważny przez 15 minut.\n" +
                "Jeśli to nie Ty, zignoruj tę wiadomość.");
        mailSender.send(message);
    }
}
