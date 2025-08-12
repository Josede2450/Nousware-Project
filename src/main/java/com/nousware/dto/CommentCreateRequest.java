// src/main/java/com/nousware/dto/CommentCreateRequest.java
package com.nousware.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CommentCreateRequest {
    @NotBlank public String content;
    @NotNull public Integer postId;
    @NotNull public Integer userId;
    public Integer parentCommentId; // optional
}
