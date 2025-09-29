// src/main/java/com/nousware/dto/MeDto.java
package com.nousware.dto;

import com.nousware.entities.Role;
import com.nousware.entities.User;

import java.util.Set;
import java.util.stream.Collectors;

public record MeDto(
        boolean authenticated,
        Integer id,
        String email,
        String firstName,   // User.name
        String lastName,
        String phone,
        String picture,     // prefers avatarUrl, falls back to pictureUrl
        Set<String> roles   // from Role.roleName
) {
    public static MeDto from(User u) {
        // Prefer user-uploaded avatar, otherwise OAuth picture
        String picture = (u.getAvatarUrl() != null && !u.getAvatarUrl().isBlank())
                ? u.getAvatarUrl()
                : u.getPictureUrl();

        // Map Role.roleName -> Set<String>
        Set<String> roleNames = (u.getRoles() == null) ? Set.of()
                : u.getRoles().stream()
                .map(Role::getRoleName)   // <- this matches your Role entity
                .filter(n -> n != null && !n.isBlank())
                .collect(Collectors.toSet());

        return new MeDto(
                true,
                u.getUserId(),
                u.getEmail(),
                u.getName(),      // your "first name" field
                u.getLastName(),
                u.getPhone(),
                picture,
                roleNames
        );
    }
}
