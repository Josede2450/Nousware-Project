package com.nousware.security;

import com.nousware.service.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
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
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
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
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource() {
        org.springframework.web.cors.CorsConfiguration cors = new org.springframework.web.cors.CorsConfiguration();

        // Orígenes explícitos desde config
        List<String> origins = Arrays.stream(corsAllowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();

        // ✅ Usar patterns para permitir todos los subdominios de vercel.app
        cors.setAllowedOriginPatterns(List.of("https://*.vercel.app", "http://localhost:3000"));
        cors.getAllowedOriginPatterns().addAll(origins);

        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of(
                "Content-Type", "Authorization", "X-Requested-With",
                "X-XSRF-TOKEN", "Accept", "Origin"
        ));
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
        CsrfTokenRequestAttributeHandler csrfRequestHandler = new CsrfTokenRequestAttributeHandler();
        csrfRequestHandler.setCsrfRequestAttributeName("_csrf");

        CookieCsrfTokenRepository csrfRepo = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfRepo.setCookiePath("/");
        csrfRepo.setSecure(true); // ✅ Secure cookies in production

        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfRepo)
                        .csrfTokenRequestHandler(csrfRequestHandler)
                        .ignoringRequestMatchers(
                                "/api/contact", "/api/contact/**",
                                "/actuator/health", "/actuator/info"
                        )
                )
                .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)
                .headers(h -> h
                        .contentTypeOptions(Customizer.withDefaults())
                        .frameOptions(f -> f.deny())
                        .referrerPolicy(r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                )
                .securityContext(sc -> sc.requireExplicitSave(false))
                .requestCache(RequestCacheConfigurer::disable)
                .sessionManagement(sm -> sm
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .sessionFixation(sf -> sf.migrateSession())
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/register",
                                "/api/auth/verify",
                                "/api/auth/login",
                                "/api/auth/login/google",
                                "/api/auth/forgot-password",
                                "/api/auth/reset-password",
                                "/api/auth/resend-verification",
                                "/oauth2/**", "/login/**",
                                "/actuator/health", "/actuator/info"
                        ).permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/services/**",
                                "/api/categories/**",
                                "/api/faqs/**",
                                "/api/testimonials/**",
                                "/api/posts/**",
                                "/api/comments/**",
                                "/api/tags/**"
                        ).permitAll()
                        .requestMatchers("/api/services/**",
                                "/api/categories/**",
                                "/api/faqs/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST,   "/api/testimonials/**").authenticated()
                        .requestMatchers(HttpMethod.PUT,    "/api/testimonials/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH,  "/api/testimonials/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/testimonials/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST,   "/api/posts/**", "/api/comments/**").authenticated()
                        .requestMatchers(HttpMethod.PUT,    "/api/posts/**", "/api/comments/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH,  "/api/posts/**", "/api/comments/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/posts/**", "/api/comments/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/contact").permitAll()
                        .requestMatchers("/api/contact/**").hasRole("ADMIN")
                        .requestMatchers("/api/users/me", "/api/users/me/**").authenticated()
                        .requestMatchers("/api/users/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth -> oauth
                        .userInfoEndpoint(u -> u.oidcUserService(dbRoleMappingOidcUserService))
                        .successHandler(successHandler)
                        .failureHandler(failureHandler)
                )
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .logoutSuccessHandler((req, res, auth) ->
                                res.setStatus(HttpServletResponse.SC_NO_CONTENT))
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(unauthorizedJson())
                        .accessDeniedHandler(forbiddenJson())
                )
                .authenticationProvider(authenticationProvider())
                .httpBasic(b -> b.disable());

        return http.build();
    }

    static final class CsrfCookieFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(
                HttpServletRequest request,
                HttpServletResponse response,
                FilterChain filterChain) throws ServletException, IOException {

            CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
            if (token != null) {
                Cookie cookie = new Cookie("XSRF-TOKEN", token.getToken());
                cookie.setPath("/");
                cookie.setHttpOnly(false);
                cookie.setSecure(true);
                cookie.setAttribute("SameSite", "None");
                response.addCookie(cookie);
            }

            filterChain.doFilter(request, response);
        }
    }
}
