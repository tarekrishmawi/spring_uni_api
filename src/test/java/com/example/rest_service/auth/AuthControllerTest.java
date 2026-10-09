package com.example.rest_service.auth;

import com.example.rest_service.auth.dto.LoginRequest;
import com.example.rest_service.auth.dto.LoginResponse;

import tools.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /*
     * AuthController depends on AuthService.
     */
    @MockitoBean
    private AuthService authService;

    /*
     * SecurityConfig depends on JwtAuthenticationFilter.
     *
     * Even though the security filters are disabled for this
     * controller test, SecurityConfig is still loaded by
     * @WebMvcTest, so the bean must exist.
     */
    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void loginReturns200() throws Exception {

        // Arrange
        LoginRequest request = new LoginRequest(
                "admin",
                "Admin123!"
        );

        LoginResponse response = new LoginResponse(
                "test-access-token",
                "test-refresh-token",
                "Bearer",
                900000L,
                "admin",
                "ADMIN"
        );

        when(authService.login(any(LoginRequest.class)))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(
                post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(request)
                        )
        )
        .andExpect(status().isOk())
        .andExpect(
                jsonPath("$.accessToken")
                        .value("test-access-token")
        )
        .andExpect(
        jsonPath("$.refreshToken")
        .value("test-refresh-token")
        )
        .andExpect(
                jsonPath("$.tokenType")
                        .value("Bearer")
        )
        .andExpect(
                jsonPath("$.expiresIn")
                        .value(900000)
        )
        .andExpect(
                jsonPath("$.username")
                        .value("admin")
        )
        .andExpect(
                jsonPath("$.role")
                        .value("ADMIN")
        );
    }

    @Test
    void loginRejectsBlankUsername() throws Exception {

        // Arrange
        LoginRequest request = new LoginRequest(
                "",
                "Admin123!"
        );

        // Act & Assert
        mockMvc.perform(
                post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(request)
                        )
        )
        .andExpect(status().isBadRequest());
    }

    @Test
    void loginRejectsBlankPassword() throws Exception {

        // Arrange
        LoginRequest request = new LoginRequest(
                "admin",
                ""
        );

        // Act & Assert
        mockMvc.perform(
                post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(request)
                        )
        )
        .andExpect(status().isBadRequest());
    }
}