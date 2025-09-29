package com.nousware.security;

import com.nousware.entities.Role;                                           // Role entity for mapping authorities
import com.nousware.entities.User;                                           // Local User entity
import com.nousware.service.UserService;                                     // Service that writes/reads users
import org.springframework.security.core.GrantedAuthority;                   // Spring Security authority type
import org.springframework.security.core.authority.SimpleGrantedAuthority;   // Concrete authority
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest; // OIDC request type
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService; // Default OIDC user loader
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;   // Default OIDC user impl
import org.springframework.security.oauth2.core.oidc.user.OidcUser;          // OIDC user contract
import org.springframework.stereotype.Component;                              // Spring bean stereotype

import java.util.HashSet;                                                    // For merging authorities
import java.util.Set;                                                        // Authority set

@Component
public class DbRoleMappingOidcUserService extends OidcUserService {

    private final UserService userService;

    public DbRoleMappingOidcUserService(UserService userService) {
        this.userService = userService;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional // <-- read-write tx keeps session open
    public OidcUser loadUser(OidcUserRequest userRequest) {
        OidcUser oidc = super.loadUser(userRequest);

        String email   = oidc.getEmail();
        String subject = oidc.getSubject();
        String given   = oidc.getGivenName();
        String family  = oidc.getFamilyName();
        String name    = ((given != null ? given : "") + " " + (family != null ? family : "")).trim();
        if (name.isBlank()) name = oidc.getFullName();

        // write inside same tx
        userService.upsertGoogleUser(subject, email, name, oidc.getPicture());

        // read inside same tx (session is open)
        User local = (email != null)
                ? userService.getByEmailOrThrow(email)
                : userService.findByEmailOrGoogleSub(null, subject)
                .orElseThrow(() -> new IllegalStateException("OIDC user not found after upsert"));

        // initialize roles while session is open
        if (local.getRoles() != null) local.getRoles().size();

        var merged = new java.util.HashSet<GrantedAuthority>(oidc.getAuthorities());
        if (local.getRoles() != null) {
            for (Role r : local.getRoles()) {
                if (r == null || r.getRoleName() == null) continue;
                String up = r.getRoleName().trim().toUpperCase();
                if (up.isBlank()) continue;
                merged.add(new SimpleGrantedAuthority(up.startsWith("ROLE_") ? up : "ROLE_" + up));
                merged.add(new SimpleGrantedAuthority(up.startsWith("ROLE_") ? up.substring(5) : up));
            }
        }

        String nameAttr = (email != null && !email.isBlank()) ? "email" : "sub";
        return new DefaultOidcUser(merged, oidc.getIdToken(), oidc.getUserInfo(), nameAttr);
    }
}
