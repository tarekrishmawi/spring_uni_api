package com.example.rest_service.student;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record StudentResponse(
    Long id,
    String studentNumber,
    String firstName,
    String lastName,
    String email,
    LocalDate dateOfBirth,
    String phone,
    String status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}