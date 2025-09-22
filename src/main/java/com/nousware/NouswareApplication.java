package com.nousware;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Map;

@SpringBootApplication
public class NouswareApplication {

	public static void main(String[] args) {
		// Load .env from project root (ignore if missing/malformed so prod can rely on real env vars)
		Dotenv dotenv = Dotenv.configure()
				.ignoreIfMissing()
				.ignoreIfMalformed()
				.load();

		// 1) Simple pass-throughs (raw env keys you may reference in application.properties)
		setIfPresent(dotenv, "DB_URL");
		setIfPresent(dotenv, "DB_USERNAME");
		setIfPresent(dotenv, "DB_PASSWORD");
		setIfPresent(dotenv, "JPA_DDL_AUTO");
		setIfPresent(dotenv, "JPA_SHOW_SQL");
		setIfPresent(dotenv, "SERVER_PORT");

		// Mail vars (still expose raw keys for ${MAIL_*} placeholders)
		setIfPresent(dotenv, "MAIL_HOST");
		setIfPresent(dotenv, "MAIL_PORT");
		setIfPresent(dotenv, "MAIL_USERNAME");
		setIfPresent(dotenv, "MAIL_PASSWORD");
		setIfPresent(dotenv, "MAIL_FROM");

		setIfPresent(dotenv, "APP_BACKEND_BASE_URL");

		// 2) Google OAuth — map your friendly env names to Spring property keys
		Map<String, String> oauthMappings = Map.of(
				"GOOGLE_CLIENT_ID", "spring.security.oauth2.client.registration.google.client-id",
				"GOOGLE_CLIENT_SECRET", "spring.security.oauth2.client.registration.google.client-secret",
				"GOOGLE_SCOPES", "spring.security.oauth2.client.registration.google.scope",
				"GOOGLE_REDIRECT_URI", "spring.security.oauth2.client.registration.google.redirect-uri",
				"OAUTH_GOOGLE_ISSUER_URI", "spring.security.oauth2.client.provider.google.issuer-uri"
		);
		setAllIfPresent(dotenv, oauthMappings);

		// 2b) Mail mappings: map .env → Spring Boot mail properties
		Map<String, String> mailMappings = Map.of(
				"MAIL_HOST", "spring.mail.host",
				"MAIL_PORT", "spring.mail.port",
				"MAIL_USERNAME", "spring.mail.username",
				"MAIL_PASSWORD", "spring.mail.password",
				"MAIL_FROM", "spring.mail.from"
		);
		setAllIfPresent(dotenv, mailMappings);

		// 2c) Database mappings: map .env → Spring Boot datasource properties
		Map<String, String> dbMappings = Map.of(
				"DB_URL", "spring.datasource.url",
				"DB_USERNAME", "spring.datasource.username",
				"DB_PASSWORD", "spring.datasource.password"
		);
		setAllIfPresent(dotenv, dbMappings);

		SpringApplication.run(NouswareApplication.class, args);
	}

	/**
	 * If key exists in .env, expose it as a System property with the same key.
	 * Useful when application.properties uses ${KEY}.
	 */
	private static void setIfPresent(Dotenv dotenv, String key) {
		String value = dotenv.get(key);
		if (value != null && !value.isBlank()) {
			// Do not overwrite if already provided via real ENV/JVM arg
			if (System.getProperty(key) == null) {
				System.setProperty(key, value);
			}
		}
	}

	/**
	 * Map EnvKey -> SpringPropertyKey and set the Spring property if present in .env.
	 */
	private static void setAllIfPresent(Dotenv dotenv, Map<String, String> mappings) {
		mappings.forEach((envKey, springKey) -> {
			String value = dotenv.get(envKey);
			if (value != null && !value.isBlank()) {
				if (System.getProperty(springKey) == null) {
					System.setProperty(springKey, value);
				}
			}
		});
	}
}
