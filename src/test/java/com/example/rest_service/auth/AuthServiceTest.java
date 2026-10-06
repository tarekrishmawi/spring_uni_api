package com.example.rest_service.auth;

import com.example.rest_service.auth.dto.LoginRequest;
import com.example.rest_service.auth.dto.LoginResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private JwtProperties jwtProperties;

    @InjectMocks
    private AuthService authService;

    private User admin;

    @BeforeEach
    void setUp() {

        admin = new User();

        admin.setUsername("admin");
        admin.setEmail("admin@university.local");
        admin.setPassword("encoded-password");
        admin.setRole(Role.ADMIN);
        admin.setEnabled(true);
    }

    @Test
    void loginReturnsAccessTokenForValidCredentials() {

        LoginRequest request =
            new LoginRequest(
                "admin",
                "Admin123!"
            );

        when(userRepository.findByUsername("admin"))
            .thenReturn(Optional.of(admin));

        when(jwtService.generateAccessToken(admin))
            .thenReturn("test-access-token");

        when(jwtProperties.getAccessTokenExpiration())
            .thenReturn(900000L);

        LoginResponse response =
            authService.login(request);

        assertNotNull(response);

        assertEquals(
            "test-access-token",
            response.accessToken()
        );

        assertEquals(
            "Bearer",
            response.tokenType()
        );

        assertEquals(
            900000L,
            response.expiresIn()
        );

        assertEquals(
            "admin",
            response.username()
        );

        assertEquals(
            "ADMIN",
            response.role()
        );

        verify(authenticationManager).authenticate(
            any(UsernamePasswordAuthenticationToken.class)
        );

        verify(userRepository).findByUsername("admin");

        verify(jwtService).generateAccessToken(admin);
    }

    @Test
    void loginFailsWhenUserDoesNotExist() {

        LoginRequest request =
            new LoginRequest(
                "unknown",
                "WrongPassword"
            );

        when(userRepository.findByUsername("unknown"))
            .thenReturn(Optional.empty());

        assertThrows(
            RuntimeException.class,
            () -> authService.login(request)
        );

        verify(jwtService, never())
            .generateAccessToken(any());
    }
}