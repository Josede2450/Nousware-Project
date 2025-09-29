// src/main/java/com/nousware/controller/MeController.java
package com.nousware.controller;

import com.nousware.dto.MeDto;
import com.nousware.dto.UpdateUserRequest;
import com.nousware.entities.User;
import com.nousware.service.FileStorageService;
import com.nousware.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/users/me")
public class MeController {

    private final UserService userService;
    private final FileStorageService fileStorageService;

    public MeController(UserService userService, FileStorageService fileStorageService) {
        this.userService = userService;
        this.fileStorageService = fileStorageService;
    }

    private String principalEmail(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) return null;
        Object p = auth.getPrincipal();
        if (p instanceof UserDetails ud) return ud.getUsername(); // local login
        if (p instanceof OAuth2User ou) {
            Object email = ou.getAttributes().get("email");
            return email != null ? email.toString() : null;
        }
        return null;
    }

    @GetMapping
    public ResponseEntity<MeDto> me(Authentication auth) {
        String email = principalEmail(auth);
        if (email == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        User u = userService.getByEmailOrThrow(email);
        return ResponseEntity.ok(MeDto.from(u));
    }

    @PutMapping
    public ResponseEntity<MeDto> updateMe(Authentication auth,
                                          @RequestBody UpdateUserRequest req) {
        String email = principalEmail(auth);
        if (email == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        User u = userService.getByEmailOrThrow(email);
        User updated = userService.updateUser(u.getUserId(), req);
        return ResponseEntity.ok(MeDto.from(updated));
    }

    @PostMapping("/avatar")
    public ResponseEntity<Map<String, String>> uploadAvatar(Authentication auth,
                                                            @RequestParam("file") MultipartFile file) {
        String email = principalEmail(auth);
        if (email == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        User u = userService.getByEmailOrThrow(email);
        String url = fileStorageService.storeUserAvatar(u.getUserId(), file);
        userService.updateAvatar(u.getUserId(), url);
        return ResponseEntity.ok(Map.of("url", url));
    }
}
