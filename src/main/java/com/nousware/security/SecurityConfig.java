package com.nousware.security;

import com.nousware.service.CustomUserDetailsService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.RequestCacheConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.util.Arrays;
import java.util.List;

@EnableMethodSecurity
@Configuration(proxyBeanMethods = false)
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final OAuth2JsonFailureHandler failureHandler;
    private final OAuth2JsonSuccessHandler successHandler;
    private final PasswordEncoder passwordEncoder;
    private final DbRoleMappingOidcUserService dbRoleMappingOidcUserService; // OIDC only

    @Value("${app.cors.allowed-origins:http://localhost:3000}")
    private String corsAllowedOrigins;

    public SecurityConfig(
            CustomUserDetailsService userDetailsService,
            OAuth2JsonFailureHandler failureHandler,
            OAuth2JsonSuccessHandler successHandler,
            PasswordEncoder passwordEncoder,
            DbRoleMappingOidcUserService dbRoleMappingOidcUserService
    ) {
        this.userDetailsService = userDetailsService;
        this.failureHandler = failureHandler;
        this.successHandler = successHandler;
        this.passwordEncoder = passwordEncoder;
        this.dbRoleMappingOidcUserService = dbRoleMappingOidcUserService;
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource() {
        org.springframework.web.cors.CorsConfiguration cors = new org.springframework.web.cors.CorsConfiguration();

        List<String> origins = Arrays.stream(corsAllowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();

        cors.setAllowedOrigins(origins);
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Content-Type","Authorization","X-Requested-With","X-XSRF-TOKEN","Accept","Origin"));
        cors.setExposedHeaders(List.of("Set-Cookie"));
        cors.setAllowCredentials(true);
        cors.setMaxAge(3600L);

        org.springframework.web.cors.UrlBasedCorsConfigurationSource source =
                new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);
        return source;
    }

    private AuthenticationEntryPoint unauthorizedJson() {
        return (request, response, authException) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"status\":401,\"error\":\"UNAUTHORIZED\"}");
        };
    }

    private AccessDeniedHandler forbiddenJson() {
        return (request, response, accessDeniedException) -> {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"status\":403,\"error\":\"FORBIDDEN\"}");
        };
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .securityContext(sc -> sc.requireExplicitSave(false))
                .requestCache(RequestCacheConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST,
                                "/api/auth/login",
                                "/api/auth/forgot-password",
                                "/api/auth/reset-password").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/auth/verify",
                                "/api/auth/login/google",
                                "/oauth2/**",
                                "/login/**").permitAll()
                        .requestMatchers(
                                "/api/auth/register",
                                "/api/auth/verify",
                                "/api/auth/login",
                                "/api/auth/login/google",
                                "/api/auth/forgot-password",
                                "/api/auth/reset-password").permitAll()

                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        .requestMatchers(HttpMethod.GET, "/api/services/**").permitAll()
                        .requestMatchers("/api/services/**").hasAnyAuthority("ADMIN","ROLE_ADMIN")

                        .requestMatchers(HttpMethod.GET, "/api/categories", "/api/categories/**").permitAll()
                        .requestMatchers("/api/categories/**").hasAnyAuthority("ADMIN","ROLE_ADMIN")

                        .requestMatchers(HttpMethod.GET, "/api/faqs", "/api/faqs/**").permitAll()
                        .requestMatchers("/api/faqs/**").hasAnyAuthority("ADMIN","ROLE_ADMIN")

                        .requestMatchers(HttpMethod.GET, "/api/testimonials", "/api/testimonials/**").permitAll()
                        .requestMatchers("/api/testimonials/**").authenticated()

                        .requestMatchers(HttpMethod.POST, "/api/contact").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/contact", "/api/contact/**").hasAnyAuthority("ADMIN","ROLE_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/contact/**").hasAnyAuthority("ADMIN","ROLE_ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/contact/**").hasAnyAuthority("ADMIN","ROLE_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/contact/**").hasAnyAuthority("ADMIN","ROLE_ADMIN")

                        .requestMatchers("/api/users/me", "/api/users/me/**").authenticated()
                        .requestMatchers("/api/users/**").hasAnyAuthority("ADMIN","ROLE_ADMIN")

                        .requestMatchers(
                                "/api/projects/**",
                                "/api/addresses/**",
                                "/api/posts/**",
                                "/api/tags/**",
                                "/api/post-likes/**",
                                "/api/comments/**",
                                "/api/comment-likes/**",
                                "/actuator/health",
                                "/actuator/info").permitAll()

                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth -> oauth
                        .userInfoEndpoint(u -> u
                                .oidcUserService(dbRoleMappingOidcUserService) // Google OIDC
                        )
                        .successHandler(successHandler)
                        .failureHandler(failureHandler)
                )
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .deleteCookies("JSESSIONID")
                        .logoutSuccessHandler((req, res, auth) -> res.setStatus(HttpServletResponse.SC_NO_CONTENT))
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(unauthorizedJson())
                        .accessDeniedHandler(forbiddenJson())
                )
                .authenticationProvider(authenticationProvider())
                .httpBasic(basic -> basic.disable());

        return http.build();
    }
}
