package com.example.rest_service.student;

import com.example.rest_service.auth.JwtAuthenticationFilter;

import tools.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StudentController.class)
class StudentControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockitoBean
        private StudentService service;

        @MockitoBean
        private JwtAuthenticationFilter jwtAuthenticationFilter;

        @Test
        void createStudentReturns201() throws Exception {

                // Arrange
                StudentResponse response = new StudentResponse(
                                1L,
                                "20260001",
                                "Ahmad",
                                "Khalil",
                                "ahmad@example.com",
                                null,
                                "0599000000",
                                "ACTIVE",
                                null,
                                null);

                when(service.create(any(StudentRequest.class)))
                                .thenReturn(response);

                StudentRequest request = new StudentRequest(
                                "20260001",
                                "Ahmad",
                                "Khalil",
                                "ahmad@example.com",
                                null,
                                "0599000000");

                // Act & Assert
                mockMvc.perform(
                                post("/api/students")
                                                .with(user("admin").roles("ADMIN"))
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(
                                                                objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.id").value(1))
                                .andExpect(
                                                jsonPath("$.studentNumber")
                                                                .value("20260001"))
                                .andExpect(
                                                jsonPath("$.firstName")
                                                                .value("Ahmad"))
                                .andExpect(
                                                jsonPath("$.lastName")
                                                                .value("Khalil"))
                                .andExpect(
                                                jsonPath("$.email")
                                                                .value("ahmad@example.com"));
        }

        @Test
        void createStudentRejectsInvalidEmail() throws Exception {

                // Arrange
                StudentRequest request = new StudentRequest(
                                "20260002",
                                "Ahmad",
                                "Khalil",
                                "not-an-email",
                                null,
                                "0599000000");

                // Act & Assert
                mockMvc.perform(
                                post("/api/students")
                                                .with(user("admin").roles("ADMIN"))
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(
                                                                objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(
                                                jsonPath("$.message")
                                                                .value("Validation failed"))
                                .andExpect(
                                                jsonPath("$.validationErrors.email")
                                                                .exists());
        }

        @Test
        void getMissingStudentReturns404() throws Exception {

                // Arrange
                when(service.findById(999L))
                                .thenThrow(
                                                new ResponseStatusException(
                                                                HttpStatus.NOT_FOUND,
                                                                "Student not found"));

                // Act & Assert
                mockMvc.perform(
                                get("/api/students/999")
                                                .with(user("admin").roles("ADMIN")))
                                .andExpect(status().isNotFound())
                                .andExpect(
                                                jsonPath("$.message")
                                                                .value("Student not found"));
        }

        @Test
        void duplicateStudentNumberReturns409() throws Exception {

                // Arrange
                StudentRequest request = new StudentRequest(
                                "20260001",
                                "Ahmad",
                                "Khalil",
                                "new@example.com",
                                null,
                                "0599000000");

                when(service.create(any(StudentRequest.class)))
                                .thenThrow(
                                                new ResponseStatusException(
                                                                HttpStatus.CONFLICT,
                                                                "Student number already exists"));

                // Act & Assert
                mockMvc.perform(
                                post("/api/students")
                                                .with(user("admin").roles("ADMIN"))
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(
                                                                objectMapper.writeValueAsString(request)))
                                .andExpect(status().isConflict())
                                .andExpect(
                                                jsonPath("$.message")
                                                                .value(
                                                                                "Student number already exists"));
        }

        @Test
        void rejectUnsupportedSortField() throws Exception {

                mockMvc.perform(
                                get("/api/students")
                                                .with(user("admin").roles("ADMIN"))
                                                .param("sortBy", "password"))
                                .andExpect(status().isBadRequest())
                                .andExpect(
                                                jsonPath("$.message")
                                                                .value(
                                                                                "Unsupported sort field: password"));
        }
}