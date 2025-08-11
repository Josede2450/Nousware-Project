package com.nousware.service;

import com.nousware.service.EmailService;
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
}
