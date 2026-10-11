package com.example.rest_service.auth;

public record LoginResult(
    String accessToken,
    String refreshToken,
    String tokenType,
    long expiresIn,
    String username,
    String role
) {}