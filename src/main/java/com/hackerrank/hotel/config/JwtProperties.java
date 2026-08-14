package com.hackerrank.hotel.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Type-safe alternative to scattering @Value("${app.security.jwt.*}") around:
 * all JWT settings bind into one validated, immutable record. Registered by
 * @ConfigurationPropertiesScan on the main application class. If the secret
 * is shorter than 32 chars (HS256 minimum), the app fails at startup instead
 * of failing on the first login.
 */
@Validated
@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(
        @Size(min = 32, message = "JWT secret must be at least 32 characters (256 bits) for HS256")
        String secret,
        @Min(1)
        long expirationMinutes) {
}
