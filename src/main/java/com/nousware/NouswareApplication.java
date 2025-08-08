package com.nousware;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class NouswareApplication {

	public static void main(String[] args) {
		// Load .env from project root
		Dotenv dotenv = Dotenv.configure()
				.ignoreIfMissing()
				.ignoreIfMalformed()
				.load();

		// DB / Server
		setIfPresent(dotenv, "DB_URL");
		setIfPresent(dotenv, "DB_USERNAME");
		setIfPresent(dotenv, "DB_PASSWORD");
		setIfPresent(dotenv, "JPA_DDL_AUTO");
		setIfPresent(dotenv, "JPA_SHOW_SQL");
		setIfPresent(dotenv, "SERVER_PORT");

		// Mail (needed for Spring Mail placeholders)
		setIfPresent(dotenv, "MAIL_HOST");
		setIfPresent(dotenv, "MAIL_PORT");
		setIfPresent(dotenv, "MAIL_USERNAME");
		setIfPresent(dotenv, "MAIL_PASSWORD");
		setIfPresent(dotenv, "MAIL_FROM");

		// Optional: backend base URL for verify links (if you use it)
		setIfPresent(dotenv, "APP_BACKEND_BASE_URL");

		SpringApplication.run(NouswareApplication.class, args);
	}

	private static void setIfPresent(Dotenv dotenv, String key) {
		String value = dotenv.get(key);
		if (value != null && !value.isBlank()) {
			System.setProperty(key, value);
		}
	}
}
