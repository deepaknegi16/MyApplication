package com.hackerrank.hotel.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * Stateless HTTP Basic security.
 *
 * - Reads (GET /hotel, /search) need any authenticated user (USER or ADMIN).
 * - DELETE /hotel needs ADMIN.
 * - Swagger UI, OpenAPI docs, H2 console and health/info probes are public.
 * - Credentials come from properties so they can be injected via environment
 *   variables in production (APP_SECURITY_*); never commit real secrets.
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
                        .requestMatchers(
                                "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**",
                                "/h2-console/**",
                                "/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers("/actuator/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/hotel/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/hotel/**", "/search/**").hasAnyRole("USER", "ADMIN")
                        .anyRequest().authenticated())
                .httpBasic(withDefaults());
        return http.build();
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
