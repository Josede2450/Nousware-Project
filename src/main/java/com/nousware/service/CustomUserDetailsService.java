package com.nousware.service;

import com.nousware.entities.User;
import com.nousware.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class CustomUserDetailsService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(CustomUserDetailsService.class);

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
        log.info("CustomUserDetailsService initialized");
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        if (email == null) {
            throw new UsernameNotFoundException("Email is required");
        }
        String normalized = email.trim().toLowerCase();

        User user = userRepository.findByEmailIgnoreCase(normalized)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + normalized));

        if (!user.isEnable()) {
            throw new DisabledException("User account is not verified yet");
        }

        Collection<? extends GrantedAuthority> authorities = getAuthorities(user);
        List<String> roleNames = authorities.stream().map(GrantedAuthority::getAuthority).toList();
        log.debug("Loaded user={}, enable={}, authorities={}", user.getEmail(), user.isEnable(), roleNames);

        // We already checked 'enable', so mark as not disabled here.
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword() == null ? "{noop}" : user.getPassword())
                .authorities(authorities)
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(false)
                .build();
    }

    private Collection<? extends GrantedAuthority> getAuthorities(User user) {
        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            // Safe default so authenticated users still have a role
            return Collections.singletonList(new SimpleGrantedAuthority("ROLE_CLIENT"));
        }

        return user.getRoles().stream()
                .map(r -> r == null ? null : r.getRoleName())
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(name -> name.startsWith("ROLE_") ? name : "ROLE_" + name)
                .distinct()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }
}
