package com.nousware.service;

import com.nousware.entities.ContactForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final JavaMailSender mailSender;

    // All localhost for dev
    private final String backendBaseUrl = "http://localhost:8080";   // API host
    private final String from = "noreply@nousware.dev";

    // Where contact notifications should be sent (change as needed)
    private final String contactNotifyTo = "admin@nousware.dev";

    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendVerificationEmail(String to, String token) {
        String link = backendBaseUrl + "/api/auth/verify?token=" + token;

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(from);
        msg.setTo(to);
        msg.setSubject("Verify your Nousware account");
        msg.setText(
                "Confirm your email to activate your account.\n\n" +
                        "Verification link:\n" + link + "\n\n" +
                        "This link expires in 15 minutes."
        );

        mailSender.send(msg);
        log.info("Verification email sent to {}", to);
        log.info("DEV verification link: {}", link);
    }

    @Override
    public void sendPasswordResetEmail(String to, String token) {
        // Dev-only helper link (note: your reset endpoint is POST; this link is for reference/logs)
        String devLink = backendBaseUrl + "/api/auth/reset-password?token=" + token;

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(from);
        msg.setTo(to);
        msg.setSubject("Reset your password");
        msg.setText(
                "We received a request to reset your password.\n\n" +
                        "Use this token in your API call:\n" +
                        token + "\n\n" +
                        "POST " + backendBaseUrl + "/api/auth/reset-password\n" +
                        "Body JSON: { \"token\": \"" + token + "\", \"newPassword\": \"<your-new-password>\" }\n\n" +
                        "This token expires in 15 minutes."
        );

        mailSender.send(msg);
        log.info("Password reset email sent to {}", to);
        log.info("DEV reset reference link (POST required): {}", devLink);
    }

    // ===== New: contact form notification =====
    @Override
    public void sendContactNotification(ContactForm form) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(from);
        msg.setTo(contactNotifyTo);
        msg.setSubject("New contact form submission");

        String body = """
                A new contact form was submitted.

                Name: %s
                Email: %s
                Phone: %s
                Created At: %s

                Message:
                %s
                """.formatted(
                safe(form.getName()),
                safe(form.getEmail()),
                safe(form.getPhone()),
                String.valueOf(form.getCreatedAt()),
                safe(form.getMessage())
        );

        msg.setText(body);
        mailSender.send(msg);
        log.info("Contact notification sent to {}", contactNotifyTo);
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}
