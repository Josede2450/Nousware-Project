// src/main/java/com/nousware/service/LocalFileStorageService.java
package com.nousware.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.*;
import java.util.Locale;

@Service // <-- this is the bean MeController needs
public class LocalFileStorageService implements FileStorageService {

    @Value("${app.upload.base-dir:uploads}")
    private String baseDir;              // folder on disk (relative or absolute)

    @Value("${app.upload.public-prefix:/uploads}")
    private String publicPrefix;         // URL prefix exposed to clients

    @Override
    public String storeUserAvatar(Integer userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Empty file");
        }
        String ext = StringUtils.getFilenameExtension(file.getOriginalFilename());
        ext = (ext == null ? "" : "." + ext.toLowerCase(Locale.ROOT));

        String filename = "avatar-" + System.currentTimeMillis() + ext;
        Path userDir = Paths.get(baseDir, "avatars", String.valueOf(userId))
                .toAbsolutePath().normalize();

        try {
            Files.createDirectories(userDir);
            Path dest = userDir.resolve(filename);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
            }
            // URL returned to the frontend
            return publicPrefix + "/avatars/" + userId + "/" + filename;
        } catch (Exception e) {
            throw new RuntimeException("Failed to store avatar", e);
        }
    }
}
