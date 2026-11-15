package com.example.dentalbackend.dto;

public record LoginResponse(
        String accessToken,
        String token,
        String tokenType,
        long expiresIn
) {
}
