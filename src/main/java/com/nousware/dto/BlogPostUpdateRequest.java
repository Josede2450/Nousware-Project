// src/main/java/com/nousware/dto/BlogPostUpdateRequest.java
package com.nousware.dto;

import java.util.Set;

public class BlogPostUpdateRequest {
    public String title;                    // Optional new title
    public String content;                  // Optional new content
    public String slug;                     // Optional new slug (kept unique)
    public Set<Integer> tagIds;             // Optional full replacement of tags
}
