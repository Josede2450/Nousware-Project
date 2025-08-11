package com.nousware.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nousware.entities.User;          // <- your entity package
import com.nousware.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class OAuth2JsonSuccessHandler implements AuthenticationSuccessHandler {

    private final ObjectMapper mapper = new ObjectMapper();
    private final UserService userService;

    public OAuth2JsonSuccessHandler(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest req,
                                        HttpServletResponse res,
                                        Authentication authentication) throws IOException {
        OAuth2User principal = (OAuth2User) authentication.getPrincipal();

        // 1) Extract profile
        String sub = getAttr(principal, "sub");
        String email = getAttr(principal, "email");
        String name = coalesce(getAttr(principal, "name"),
                (getAttr(principal, "given_name") + " " + getAttr(principal, "family_name")).trim());
        String picture = getAttr(principal, "picture");
        boolean emailVerified = Boolean.parseBoolean(String.valueOf(
                principal.getAttributes().getOrDefault("email_verified", "true")));

        if (email == null || email.isBlank()) {
            writeError(res, HttpServletResponse.SC_BAD_REQUEST, "google_missing_email",
                    "Google did not provide an email address for this account.");
            return;
        }
        if (!emailVerified) {
            writeError(res, HttpServletResponse.SC_FORBIDDEN, "email_not_verified",
                    "Google email is not verified.");
            return;
        }

        // 2) Upsert user
        User before = userService.findByEmailOrGoogleSub(email, sub).orElse(null);
        User saved = userService.upsertGoogleUser(sub, email, name, picture);
        boolean created = (before == null);
        Instant now = Instant.now();

        // 3) Build payload
        List<String> roles = authentication.getAuthorities()
                .stream().map(GrantedAuthority::getAuthority).toList();

        Map<String, Object> payload = Map.of(
                "authenticated", true,
                "provider", "google",
                "created", created,
                "userId", saved.getUserId(),   // <— fixed
                "email", saved.getEmail(),
                "name", Objects.toString(saved.getName(), ""),
                "picture", Objects.toString(picture, ""),
                "sub", Objects.toString(sub, ""),
                "roles", roles,
                "timestamp", now.toString()
        );

        // Optional redirect support (?next=...)
        String next = req.getParameter("next");
        if (next != null && !next.isBlank()) {
            String url = next + (next.contains("?") ? "&" : "?")
                    + "authenticated=true"
                    + "&userId=" + urlEnc(String.valueOf(saved.getUserId()))
                    + "&email=" + urlEnc(saved.getEmail());
            res.sendRedirect(url);
            return;
        }

        // Default: JSON
        res.setStatus(HttpServletResponse.SC_OK);
        res.setContentType("application/json");
        mapper.writeValue(res.getOutputStream(), payload);
    }

    // ---- helpers ----
    private static String getAttr(OAuth2User u, String key) {
        Object v = u.getAttributes().get(key);
        if (v == null) return null;
        String s = String.valueOf(v).trim();
        return s.isEmpty() ? null : s;
    }

    private static String coalesce(String a, String b) {
        return (a != null && !a.isBlank()) ? a : (b != null ? b : null);
    }

    private static String urlEnc(String s) {
        return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
    }

    private void writeError(HttpServletResponse res, int status, String code, String message) throws IOException {
        res.setStatus(status);
        res.setContentType("application/json");
        mapper.writeValue(res.getOutputStream(), Map.of(
                "authenticated", false,
                "error", code,
                "message", message
        ));
    }
}
