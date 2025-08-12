// src/main/java/com/nousware/dto/CommentUpdateRequest.java
package com.nousware.dto;

import jakarta.validation.constraints.NotBlank;

public class CommentUpdateRequest {
    @NotBlank public String content;
}
