// src/main/java/com/nousware/dto/PostLikeCountResponse.java
package com.nousware.dto;

public class PostLikeCountResponse {
    public int postId;
    public long likeCount;

    public PostLikeCountResponse(int postId, long likeCount) {
        this.postId = postId;
        this.likeCount = likeCount;
    }
}
