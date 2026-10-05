package com.example.rest_service.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

    @NotBlank
    String username,

    @NotBlank
    String password

) {}