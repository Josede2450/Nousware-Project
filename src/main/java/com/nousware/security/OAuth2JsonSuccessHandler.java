// OAuth2JsonSuccessHandler.java
package com.nousware.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nousware.entities.User;
import com.nousware.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

@Component
public class OAuth2JsonSuccessHandler implements AuthenticationSuccessHandler {

    private final ObjectMapper mapper = new ObjectMapper();
    private final UserService userService;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl; // ✅ where to send users after OAuth

    public OAuth2JsonSuccessHandler(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest req,
                                        HttpServletResponse res,
                                        Authentication authentication) throws IOException {

        OAuth2User o = (OAuth2User) authentication.getPrincipal();

        String sub     = attr(o, "sub");
        String email   = attr(o, "email");
        String picture = attr(o, "picture");
        String name    = firstNonBlank(attr(o, "name"),
                ((attr(o, "given_name") == null ? "" : attr(o, "given_name")) + " " +
                        (attr(o, "family_name") == null ? "" : attr(o, "family_name"))).trim());

        // Upsert local user (will set default CLIENT role if new)
        User saved = userService.upsertGoogleUser(sub, email, name, picture);

        // ✅ Redirect back to frontend (optionally honoring ?next=)
        String next = req.getParameter("next");
        String dst  = (next != null && !next.isBlank()) ? next : frontendUrl;
        String url  = dst + (dst.contains("?") ? "&" : "?")
                + "authenticated=true"
                + "&userId=" + enc(String.valueOf(saved.getUserId()))
                + "&email="  + enc(saved.getEmail() == null ? "" : saved.getEmail())
                + "&t=" + enc(Instant.now().toString());

        res.sendRedirect(url);
    }

    private static String attr(OAuth2User u, String key) {
        Object v = u.getAttributes().get(key);
        if (v == null) return null;
        String s = String.valueOf(v).trim();
        return s.isEmpty() ? null : s;
    }

    private static String firstNonBlank(String a, String b) {
        return (a != null && !a.isBlank()) ? a : ((b != null && !b.isBlank()) ? b : null);
    }

    private static String enc(String s) {
        return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
    }
}
