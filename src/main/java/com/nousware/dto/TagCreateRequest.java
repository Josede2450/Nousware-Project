// src/main/java/com/nousware/dto/TagCreateRequest.java
package com.nousware.dto;

import jakarta.validation.constraints.NotBlank;

public class TagCreateRequest {
    @NotBlank public String name;   // Display name (e.g., "Spring Boot")
    @NotBlank public String slug;   // URL-safe unique slug (e.g., "spring-boot")
}
