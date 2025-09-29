package com.nousware.service;

import com.nousware.entities.ContactForm;
import jakarta.mail.internet.InternetAddress;   // ⬅️ add this
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:${MAIL_USERNAME}}")
    private String from;

    @Value("${app.contact.notify-to:${MAIL_USERNAME}}")
    private String contactNotifyTo;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendBaseUrl;

    // De-dupe admin notification (same email+message within window)
    private static final long ADMIN_DEDUPE_WINDOW_MS = TimeUnit.MINUTES.toMillis(15);
    private static final ConcurrentHashMap<String, Long> ADMIN_RECENT = new ConcurrentHashMap<>();

    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    // ----- templates -----
    private String loadTemplate(String name) throws java.io.IOException {
        var res = new ClassPathResource("templates/" + name + ".html");
        try (var in = res.getInputStream()) {
            return StreamUtils.copyToString(in, StandardCharsets.UTF_8);
        }
    }

    private static String htmlEscape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static String safe(String s) { return s == null ? "" : s; }

    // ----- verify/reset -----
    @Override
    public void sendVerificationEmail(String to, String token) {
        try {
            String link = frontendBaseUrl + "/login?verified=true&token=" + token;
            String html = loadTemplate("verify")
                    .replace("{{link}}", link)
                    .replace("{{expiresMinutes}}", "15");
            sendHtml(to, "Verify your CKS account", html); // (optional) brand tweak
        } catch (Exception e) {
            log.error("Failed to send verification email", e);
        }
    }

    @Override
    public void sendPasswordResetEmail(String to, String token) {
        try {
            String link = frontendBaseUrl + "/auth/reset-password?token=" + token;
            String html = loadTemplate("reset")
                    .replace("{{link}}", link)
                    .replace("{{expiresMinutes}}", "15");
            sendHtml(to, "Reset your CKS password", html); // (optional) brand tweak
        } catch (Exception e) {
            log.error("Failed to send reset email", e);
        }
    }

    // ----- contact emails -----
    @Override
    public void sendContactNotification(ContactForm form) {
        try {
            // de-dupe key by email + message
            String adminKey = (safe(form.getEmail()).trim().toLowerCase() + "|" + safe(form.getMessage()).trim());
            long now = System.currentTimeMillis();
            Long last = ADMIN_RECENT.put(adminKey, now);
            if (last != null && (now - last) < ADMIN_DEDUPE_WINDOW_MS) {
                log.info("Skipped admin notification (deduped) for key hash {}", adminKey.hashCode());
                return;
            }

            String html = loadTemplate("contact")
                    .replace("{{name}}", htmlEscape(form.getName()))
                    .replace("{{email}}", htmlEscape(form.getEmail()))
                    .replace("{{phone}}", htmlEscape(form.getPhone()))
                    .replace("{{createdAt}}", htmlEscape(String.valueOf(form.getCreatedAt())))
                    .replace("{{message}}", htmlEscape(form.getMessage()));

            sendHtml(contactNotifyTo, "New contact form submission", html, form.getEmail());
        } catch (Exception e) {
            log.error("Failed to send contact notification", e);
        }
    }

    @Override
    public void sendContactConfirmation(ContactForm form) {
        try {
            String html = loadTemplate("receipt")
                    .replace("{{name}}", htmlEscape(form.getName()))
                    .replace("{{email}}", htmlEscape(form.getEmail()))
                    .replace("{{phone}}", htmlEscape(form.getPhone()))
                    .replace("{{createdAt}}", htmlEscape(String.valueOf(form.getCreatedAt())))
                    .replace("{{message}}", htmlEscape(form.getMessage()));
            sendHtml(form.getEmail(), "We received your message — CKS", html); // (optional) brand tweak
        } catch (Exception e) {
            log.error("Failed to send contact confirmation", e);
        }
    }

    // ----- common sender -----
    private void sendHtml(String to, String subject, String html) throws Exception {
        sendHtml(to, subject, html, null);
    }

    private void sendHtml(String to, String subject, String html, String replyTo) throws Exception {
        MimeMessage mime = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mime, "UTF-8");

        // ⬇️ IMPORTANT: set display name "CKS" on the From header
        helper.setFrom(new InternetAddress(from, "CKS"));

        helper.setTo(to);
        if (replyTo != null && !replyTo.isBlank()) {
            helper.setReplyTo(replyTo);
        }
        helper.setSubject(subject);
        helper.setText(html, true);
        mailSender.send(mime);
    }
}
