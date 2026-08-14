package com.hackerrank.hotel.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Stateless JWT security.
 *
 * - POST /auth/login is public and exchanges credentials for a signed HS256 token.
 * - Every other API call must carry "Authorization: Bearer <token>", validated by
 *   Spring Security's OAuth2 resource server against the same signing key.
 * - Reads (GET /hotel, /search) need any authenticated user (USER or ADMIN);
 *   DELETE /hotel needs ADMIN. Roles travel inside the token's "roles" claim.
 * - Swagger UI, OpenAPI docs, H2 console and health/info probes are public.
 * - Credentials and the JWT secret come from properties so production can inject
 *   them via environment variables (APP_SECURITY_*); never commit real secrets.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // stateless REST API with no browser form clients: CSRF does not apply
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // allow the H2 console to render its frames
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                        .requestMatchers(
                                "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**",
                                "/h2-console/**",
                                "/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers("/actuator/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/hotel/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/hotel/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/hotel/**", "/search/**", "/city/**").hasAnyRole("USER", "ADMIN")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
        return http.build();
    }

    /**
     * The token stores authorities in a "roles" claim already prefixed with
     * ROLE_, so no extra prefix is added here.
     */
    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("roles");
        authorities.setAuthorityPrefix("");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public UserDetailsService userDetailsService(
            PasswordEncoder encoder,
            @Value("${app.security.user.name}") String userName,
            @Value("${app.security.user.password}") String userPassword,
            @Value("${app.security.admin.name}") String adminName,
            @Value("${app.security.admin.password}") String adminPassword) {
        return new InMemoryUserDetailsManager(
                User.builder()
                        .username(userName)
                        .password(encoder.encode(userPassword))
                        .roles("USER")
                        .build(),
                User.builder()
                        .username(adminName)
                        .password(encoder.encode(adminPassword))
                        .roles("ADMIN")
                        .build());
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
