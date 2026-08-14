package com.hackerrank.hotel.dto;

public record TokenResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds) {
}
