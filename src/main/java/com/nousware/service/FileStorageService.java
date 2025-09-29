// src/main/java/com/nousware/service/FileStorageService.java
package com.nousware.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    /** Store the avatar and return a public URL */
    String storeUserAvatar(Integer userId, MultipartFile file);
}
