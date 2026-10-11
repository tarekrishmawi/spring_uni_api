
package com.example.rest_service.auth;

import com.example.rest_service.auth.dto.LoginRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;

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

    @Mock
    private RefreshTokenService refreshTokenService;

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
    void loginReturnsTokensForValidCredentials() {

        LoginRequest request = new LoginRequest(
                "admin",
                "Admin123!"
        );

        when(userRepository.findByUsername("admin"))
                .thenReturn(Optional.of(admin));

        when(jwtService.generateAccessToken(admin))
                .thenReturn("test-access-token");

        when(jwtProperties.getAccessTokenExpiration())
                .thenReturn(900000L);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken("test-refresh-token");
        refreshToken.setUser(admin);

        when(refreshTokenService.create(admin))
                .thenReturn(refreshToken);

        LoginResult result = authService.login(request);

        assertNotNull(result);
        assertEquals("test-access-token", result.accessToken());
        assertEquals("test-refresh-token", result.refreshToken());
        assertEquals("Bearer", result.tokenType());
        assertEquals(900000L, result.expiresIn());
        assertEquals("admin", result.username());
        assertEquals("ADMIN", result.role());

        verify(authenticationManager).authenticate(
                any(UsernamePasswordAuthenticationToken.class)
        );
        verify(userRepository).findByUsername("admin");
        verify(jwtService).generateAccessToken(admin);
        verify(refreshTokenService).create(admin);
    }

    @Test
    void loginFailsForInvalidCredentials() {

        LoginRequest request = new LoginRequest(
                "unknown",
                "WrongPassword"
        );

        when(authenticationManager.authenticate(
                any(UsernamePasswordAuthenticationToken.class)
        )).thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(
                BadCredentialsException.class,
                () -> authService.login(request)
        );

        verifyNoInteractions(userRepository);
        verifyNoInteractions(jwtService);
        verifyNoInteractions(refreshTokenService);
    }

    @Test
    void loginFailsWhenAuthenticatedUserNoLongerExists() {

        LoginRequest request = new LoginRequest(
                "admin",
                "Admin123!"
        );

        when(userRepository.findByUsername("admin"))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authService.login(request)
        );

        assertEquals(
                HttpStatus.UNAUTHORIZED,
                exception.getStatusCode()
        );

        verify(jwtService, never()).generateAccessToken(any());
        verify(refreshTokenService, never()).create(any());
    }

    @Test
    void loginRejectsDisabledUser() {

        admin.setEnabled(false);

        LoginRequest request = new LoginRequest(
                "admin",
                "Admin123!"
        );

        when(userRepository.findByUsername("admin"))
                .thenReturn(Optional.of(admin));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authService.login(request)
        );

        assertEquals(
                HttpStatus.UNAUTHORIZED,
                exception.getStatusCode()
        );

        verify(jwtService, never()).generateAccessToken(any());
        verify(refreshTokenService, never()).create(any());
    }

    @Test
    void refreshReturnsNewAccessAndRefreshTokens() {

        RefreshToken oldToken = new RefreshToken();
        oldToken.setToken("old-refresh-token");
        oldToken.setUser(admin);

        RefreshToken replacement = new RefreshToken();
        replacement.setToken("new-refresh-token");
        replacement.setUser(admin);

        when(refreshTokenService.validate("old-refresh-token"))
                .thenReturn(oldToken);

        when(refreshTokenService.create(admin))
                .thenReturn(replacement);

        when(jwtService.generateAccessToken(admin))
                .thenReturn("new-access-token");

        when(jwtProperties.getAccessTokenExpiration())
                .thenReturn(900000L);

        RefreshResult result = authService.refresh("old-refresh-token");

        assertNotNull(result);
        assertEquals("new-access-token", result.accessToken());
        assertEquals("new-refresh-token", result.refreshToken());
        assertEquals("Bearer", result.tokenType());
        assertEquals(900000L, result.expiresIn());
        assertEquals("admin", result.username());
        assertEquals("ADMIN", result.role());

        verify(refreshTokenService).validate("old-refresh-token");
        verify(refreshTokenService).create(admin);
        verify(jwtService).generateAccessToken(admin);
    }

    @Test
    void refreshRejectsMissingToken() {

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authService.refresh(" ")
        );

        assertEquals(
                HttpStatus.UNAUTHORIZED,
                exception.getStatusCode()
        );

        verifyNoInteractions(jwtService);
        verifyNoInteractions(userRepository);
    }

    @Test
    void refreshRejectsDisabledUser() {

        admin.setEnabled(false);

        RefreshToken oldToken = new RefreshToken();
        oldToken.setToken("old-refresh-token");
        oldToken.setUser(admin);

        when(refreshTokenService.validate("old-refresh-token"))
                .thenReturn(oldToken);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authService.refresh("old-refresh-token")
        );

        assertEquals(
                HttpStatus.UNAUTHORIZED,
                exception.getStatusCode()
        );

        verify(refreshTokenService).revoke("old-refresh-token");
        verify(refreshTokenService, never()).create(any());
        verify(jwtService, never()).generateAccessToken(any());
    }

    @Test
    void logoutRevokesProvidedRefreshToken() {

        authService.logout("test-refresh-token");

        verify(refreshTokenService).revoke("test-refresh-token");
    }

    @Test
    void logoutIgnoresMissingRefreshToken() {

        authService.logout(null);
        authService.logout(" ");

        verifyNoInteractions(refreshTokenService);
    }
}
