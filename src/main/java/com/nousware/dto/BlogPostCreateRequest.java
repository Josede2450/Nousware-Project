// src/main/java/com/nousware/dto/BlogPostCreateRequest.java
package com.nousware.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

public class BlogPostCreateRequest {
    @NotBlank public String title;          // Post title
    @NotBlank public String content;        // Post body
    @NotBlank public String slug;           // Unique slug (you can auto-generate if you want)
    @NotNull  public Integer userId;        // Author userId
    public Set<Integer> tagIds;             // Optional initial tags
}
