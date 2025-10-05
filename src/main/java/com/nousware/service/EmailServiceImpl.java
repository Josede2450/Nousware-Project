package com.nousware.service;

import com.nousware.entities.ContactForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    @Value("${BREVO_API_KEY}")
    private String brevoApiKey;  // 🔐 your Brevo API key

    @Value("${MAIL_FROM}")
    private String fromEmail;   // Verified sender email
    @Value("${app.contact.notify-to:${MAIL_FROM}}")
    private String contactNotifyTo;
    @Value("${app.frontend-url:https://cks.software}")
    private String frontendBaseUrl;

    private final RestTemplate rest = new RestTemplate();

    private static final long ADMIN_DEDUPE_WINDOW_MS = TimeUnit.MINUTES.toMillis(15);
    private static final ConcurrentHashMap<String, Long> ADMIN_RECENT = new ConcurrentHashMap<>();

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

    // ----- verification -----
    @Override
    public void sendVerificationEmail(String to, String token) {
        try {
            String link = frontendBaseUrl + "/login?verified=true&token=" + token;
            String html = loadTemplate("verify")
                    .replace("{{link}}", link)
                    .replace("{{expiresMinutes}}", "15");
            sendHtml(to, "Verify your CKS account", html);
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
            sendHtml(to, "Reset your CKS password", html);
        } catch (Exception e) {
            log.error("Failed to send password reset email", e);
        }
    }

    // ----- contact -----
    @Override
    public void sendContactNotification(ContactForm form) {
        try {
            String key = safe(form.getEmail()).trim().toLowerCase() + "|" + safe(form.getMessage()).trim();
            long now = System.currentTimeMillis();
            Long last = ADMIN_RECENT.put(key, now);
            if (last != null && (now - last) < ADMIN_DEDUPE_WINDOW_MS) {
                log.info("Skipped admin notification (deduped)");
                return;
            }

            String html = loadTemplate("contact")
                    .replace("{{name}}", htmlEscape(form.getName()))
                    .replace("{{email}}", htmlEscape(form.getEmail()))
                    .replace("{{phone}}", htmlEscape(form.getPhone()))
                    .replace("{{createdAt}}", htmlEscape(String.valueOf(form.getCreatedAt())))
                    .replace("{{message}}", htmlEscape(form.getMessage()));

            sendHtml(contactNotifyTo, "New contact form submission", html);
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
            sendHtml(form.getEmail(), "We received your message — CKS", html);
        } catch (Exception e) {
            log.error("Failed to send contact confirmation", e);
        }
    }

    // ----- Common sender (via Brevo API) -----
    private void sendHtml(String to, String subject, String html) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("sender", Map.of("email", fromEmail, "name", "CKS"));
            payload.put("to", List.of(Map.of("email", to)));
            payload.put("subject", subject);
            payload.put("htmlContent", html);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", brevoApiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            ResponseEntity<String> response =
                    rest.postForEntity("https://api.brevo.com/v3/smtp/email", entity, String.class);

            log.info("Brevo email sent [{} → {}]: {}", subject, to, response.getStatusCode());
        } catch (Exception e) {
            log.error("Failed to send email via Brevo API", e);
        }
    }
}
