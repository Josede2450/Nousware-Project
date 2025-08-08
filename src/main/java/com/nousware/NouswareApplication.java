package com.nousware;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class NouswareApplication {

	public static void main(String[] args) {

		// Load .env file from project root
		Dotenv dotenv = Dotenv.configure()
				.ignoreIfMissing()   // won't crash if missing
				.ignoreIfMalformed() // tolerant of bad lines
				.load();

		// Push all needed vars into system properties so Spring can resolve them
		setIfPresent(dotenv, "DB_URL");
		setIfPresent(dotenv, "DB_USERNAME");
		setIfPresent(dotenv, "DB_PASSWORD");
		setIfPresent(dotenv, "JPA_DDL_AUTO");
		setIfPresent(dotenv, "JPA_SHOW_SQL");
		setIfPresent(dotenv, "SERVER_PORT");

		SpringApplication.run(NouswareApplication.class, args);
	}

	private static void setIfPresent(Dotenv dotenv, String key) {
		String value = dotenv.get(key);
		if (value != null && !value.isBlank()) {
			System.setProperty(key, value);
		}
	}
}
