package com.nousware.service;

import com.nousware.entities.ContactForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    // Make "from" configurable; falls back if not set
    @Value("${app.mail.from:noreply@nousware.dev}")
    private String from;

    // Values configurable from application.properties/yml
    @Value("${app.backend-url:http://localhost:8080}")
    private String backendBaseUrl;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendBaseUrl;

    @Value("${app.contact.notify-to:admin@nousware.dev}")
    private String contactNotifyTo;

    // Allow the app to start even if the mail bean is missing
    @Autowired(required = false)
    @Nullable
    private JavaMailSender mailSender;

    private boolean mailReady() {
        if (mailSender == null) {
            log.warn("JavaMailSender not configured — skipping email send.");
            return false;
        }
        return true;
    }

    @Override
    public void sendVerificationEmail(String to, String token) {
        if (!mailReady()) return;

        String link = frontendBaseUrl + "/login?verified=true&token=" + token;

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(from);
        msg.setTo(to);
        msg.setSubject("Verify your Nousware account");
        msg.setText(
                "Confirm your email to activate your account.\n\n" +
                        "Click the link below:\n" + link + "\n\n" +
                        "This link expires in 15 minutes."
        );

        mailSender.send(msg);
        log.info("Verification email sent to {}", to);
        log.info("DEV verification link: {}", link);
    }

    @Override
    public void sendPasswordResetEmail(String to, String token) {
        if (!mailReady()) return;

        String link = frontendBaseUrl + "/auth/reset-password?token=" + token;

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(from);
        msg.setTo(to);
        msg.setSubject("Reset your password");
        msg.setText(
                "We received a request to reset your password.\n\n" +
                        "Click the link below to set a new password:\n" + link + "\n\n" +
                        "If you did not request this, please ignore this email.\n\n" +
                        "This link expires in 15 minutes."
        );

        mailSender.send(msg);
        log.info("Password reset email sent to {}", to);
        log.info("DEV reset link: {}", link);
    }

    @Override
    public void sendContactNotification(ContactForm form) {
        if (!mailReady()) return;

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
