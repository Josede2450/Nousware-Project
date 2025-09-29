package com.nousware.controller;

import com.nousware.dto.UpdateUserRequest;
import com.nousware.entities.Role;
import com.nousware.entities.User;
import com.nousware.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    // If admin-only, uncomment:
    // @PreAuthorize("hasRole('ADMIN')")
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** GET /api/users?page=0&size=20&sort=createdAt,desc */
    @GetMapping
    public ResponseEntity<Page<Map<String, Object>>> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        String[] parts = sort.split(",", 2);
        String prop = parts[0];
        Sort.Direction dir = (parts.length > 1 && "asc".equalsIgnoreCase(parts[1]))
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(dir, prop));
        Page<User> users = userService.findAll(pageable);

        List<Map<String, Object>> content = users.getContent().stream()
                .map(this::toUserMap)
                .toList();

        Page<Map<String, Object>> out = new PageImpl<>(content, pageable, users.getTotalElements());
        return ResponseEntity.ok(out);
    }

    /** GET /api/users/{id} */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getUser(@PathVariable Integer id) {
        return userService.findById(id)
                .map(u -> ResponseEntity.ok(toUserMap(u)))
                .orElse(ResponseEntity.notFound().build());
    }

    /** PUT /api/users/{id} */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateUser(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateUserRequest req
    ) {
        User updated = userService.updateUser(id, req);
        return ResponseEntity.ok(toUserMap(updated));
    }

    // ---------- mapping helpers (kept local to avoid coupling) ----------

    private Map<String, Object> toUserMap(User u) {
        List<String> roles = normalizeAndSortRoles(roleNames(u.getRoles()));
        String avatar = u.getAvatarUrl();
        String picture = u.getPictureUrl();
        String display = (avatar != null && !avatar.isBlank()) ? avatar
                : ((picture != null && !picture.isBlank()) ? picture : null);

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.getUserId());
        m.put("email", u.getEmail());
        m.put("firstName", u.getName());           // 'name' is first name in your model
        if (u.getLastName() != null) m.put("lastName", u.getLastName());
        m.put("enabled", u.isEnable());
        m.put("provider", u.getProvider());
        m.put("role", topRole(roles));
        m.put("roles", roles);
        if (avatar  != null) m.put("avatar", avatar);
        if (picture != null) m.put("picture", picture);
        if (display != null) m.put("displayPicture", display);
        m.put("phone", u.getPhone());
        m.put("gender", u.getGender());
        m.put("createdAt", u.getCreatedAt());
        m.put("updatedAt", u.getUpdatedAt());
        m.put("lastLoginAt", u.getLastLoginAt());
        return m;
    }

    private List<String> roleNames(List<Role> roles) {
        if (roles == null) return List.of();
        return roles.stream()
                .filter(r -> r != null && r.getRoleName() != null)
                .map(Role::getRoleName)
                .collect(Collectors.toList());
    }

    private String normalizeRole(String role) {
        if (role == null) return null;
        String s = role.trim().toUpperCase();
        if (s.startsWith("ROLE_")) s = s.substring(5);
        return s;
    }

    private List<String> normalizeAndSortRoles(List<String> raw) {
        return raw.stream()
                .map(this::normalizeRole)
                .filter(s -> s != null && !s.isBlank())
                .distinct()
                .sorted((a, b) -> {
                    // ADMIN first, then alphabetically
                    if ("ADMIN".equals(a) && !"ADMIN".equals(b)) return -1;
                    if (!"ADMIN".equals(a) && "ADMIN".equals(b)) return 1;
                    return a.compareTo(b);
                })
                .toList();
    }

    private String topRole(List<String> roles) {
        if (roles == null || roles.isEmpty()) return "CLIENT";
        if (roles.stream().anyMatch("ADMIN"::equalsIgnoreCase)) return "ADMIN";
        return roles.get(0);
    }
}
