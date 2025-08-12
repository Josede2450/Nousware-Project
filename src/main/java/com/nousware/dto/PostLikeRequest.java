// src/main/java/com/nousware/dto/PostLikeRequest.java
package com.nousware.dto;

import jakarta.validation.constraints.NotNull;

public class PostLikeRequest {
    @NotNull public Integer postId; // target post
    @NotNull public Integer userId; // who likes/unlikes (for now we accept from body)
}
