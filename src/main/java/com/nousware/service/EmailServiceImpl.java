package com.nousware.service.impl;

import com.nousware.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final JavaMailSender mailSender;

    // Where your backend runs; used to build the verify link
    @Value("${app.backend.base-url:http://localhost:8080}")
    private String backendBaseUrl;

    // From address
    @Value("${spring.mail.from:noreply@nousware.dev}")
    private String from;

    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendVerificationEmail(String to, String token) {
        String link = backendBaseUrl + "/api/auth/verify?token=" + token;

        String subject = "Verify your Nousware account";
        String html = """
            <div style="font-family:Arial,Helvetica,sans-serif;font-size:14px;line-height:1.6">
              <h2>Confirm your email</h2>
              <p>Click the button below to verify your account.</p>
              <p><a href="%s" 
                    style="display:inline-block;padding:10px 16px;text-decoration:none;border-radius:6px;
                           background:#0d6efd;color:#fff;text-decoration:none">Verify Email</a></p>
              <p>Or copy this link:<br>%s</p>
              <p>This link expires in 15 minutes.</p>
            </div>
            """.formatted(link, link);

        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, "UTF-8");
            helper.setTo(to);
            helper.setFrom(from);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(msg);
            log.info("Verification email sent to {}", to);
        } catch (MessagingException e) {
            // Don’t crash the flow in dev—log and also print the link for manual testing
            log.error("Failed to send verification email to {}: {}", to, e.getMessage());
            log.info("DEV fallback — verification link: {}", link);
        }
    }
}
