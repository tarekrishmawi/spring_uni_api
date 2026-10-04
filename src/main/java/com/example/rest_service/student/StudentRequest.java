package com.example.rest_service.student;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record StudentRequest(

    @NotBlank
    @Size(max = 20)
    String studentNumber,

    @NotBlank
    @Size(max = 100)
    String firstName,

    @NotBlank
    @Size(max = 100)
    String lastName,

    @NotBlank
    @Email
    @Size(max = 150)
    String email,

    @Past
    LocalDate dateOfBirth,

    @Size(max = 30)
    String phone
) {}