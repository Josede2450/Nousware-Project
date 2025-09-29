package com.nousware;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Map;

@SpringBootApplication
public class NouswareApplication {

	public static void main(String[] args) {
		Dotenv dotenv = Dotenv.configure()
				.ignoreIfMissing()
				.ignoreIfMalformed()
				.load();

		// ===== Pass-throughs you might reference in application.properties =====
		setIfPresent(dotenv, "DB_URL");
		setIfPresent(dotenv, "DB_USERNAME");
		setIfPresent(dotenv, "DB_PASSWORD");
		setIfPresent(dotenv, "JPA_DDL_AUTO");
		setIfPresent(dotenv, "JPA_SHOW_SQL");

		// Map SERVER_PORT -> Spring's server.port
		mapIfPresent(dotenv, "SERVER_PORT", "server.port");

		// ===== Mail: raw keys (if you reference ${MAIL_*}) =====
		setIfPresent(dotenv, "MAIL_HOST");
		setIfPresent(dotenv, "MAIL_PORT");
		setIfPresent(dotenv, "MAIL_USERNAME");
		setIfPresent(dotenv, "MAIL_PASSWORD");
		setIfPresent(dotenv, "MAIL_PROTOCOL");
		setIfPresent(dotenv, "MAIL_FROM"); // keep available, too

		setIfPresent(dotenv, "APP_BACKEND_BASE_URL");

		// ===== Google OAuth -> Spring keys =====
		setAllIfPresent(dotenv, Map.of(
				"GOOGLE_CLIENT_ID", "spring.security.oauth2.client.registration.google.client-id",
				"GOOGLE_CLIENT_SECRET", "spring.security.oauth2.client.registration.google.client-secret",
				"GOOGLE_SCOPES", "spring.security.oauth2.client.registration.google.scope",
				"GOOGLE_REDIRECT_URI", "spring.security.oauth2.client.registration.google.redirect-uri",
				"OAUTH_GOOGLE_ISSUER_URI", "spring.security.oauth2.client.provider.google.issuer-uri"
		));

		// ===== Mail: .env -> Spring Boot mail properties =====
		setAllIfPresent(dotenv, Map.of(
				"MAIL_HOST", "spring.mail.host",
				"MAIL_PORT", "spring.mail.port",
				"MAIL_USERNAME", "spring.mail.username",
				"MAIL_PASSWORD", "spring.mail.password",
				"MAIL_PROTOCOL", "spring.mail.protocol"
				// NOTE: do NOT map MAIL_FROM to spring.mail.from (not a standard prop)
		));

		// Use a proper custom property for default From:
		mapIfPresent(dotenv, "MAIL_FROM", "app.mail.from");

		// Enforce Gmail-required SMTP options (can also live in application.properties)
		setIfAbsent("spring.mail.properties.mail.smtp.auth", "true");
		setIfAbsent("spring.mail.properties.mail.smtp.starttls.enable", "true");
		setIfAbsent("spring.mail.properties.mail.smtp.starttls.required", "true");
		setIfAbsent("spring.mail.properties.mail.smtp.connectiontimeout", "5000");
		setIfAbsent("spring.mail.properties.mail.smtp.timeout", "5000");
		setIfAbsent("spring.mail.properties.mail.smtp.writetimeout", "5000");

		SpringApplication.run(NouswareApplication.class, args);
	}

	private static void setIfPresent(Dotenv dotenv, String key) {
		String value = dotenv.get(key);
		if (value != null && !value.isBlank() && System.getProperty(key) == null) {
			System.setProperty(key, value);
		}
	}

	private static void mapIfPresent(Dotenv dotenv, String envKey, String springKey) {
		String value = dotenv.get(envKey);
		if (value != null && !value.isBlank() && System.getProperty(springKey) == null) {
			System.setProperty(springKey, value);
		}
	}

	private static void setAllIfPresent(Dotenv dotenv, Map<String, String> mappings) {
		mappings.forEach((envKey, springKey) -> {
			String value = dotenv.get(envKey);
			if (value != null && !value.isBlank() && System.getProperty(springKey) == null) {
				System.setProperty(springKey, value);
			}
		});
	}

	private static void setIfAbsent(String key, String value) {
		if (System.getProperty(key) == null) {
			System.setProperty(key, value);
		}
	}
}
