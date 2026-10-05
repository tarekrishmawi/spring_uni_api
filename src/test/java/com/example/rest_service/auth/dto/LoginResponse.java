package com.example.rest_service.auth.dto;

public record LoginResponse(
    String accessToken,
    String tokenType,
    long expiresIn,
    String username,
    String role
) {}