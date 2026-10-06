package com.example.rest_service.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    private User user;

    @BeforeEach
    void setUp() {

        JwtProperties properties =
            new JwtProperties();

        properties.setSecret(
            "ThisIsADevelopmentTestSecretKeyThatIsAtLeast32BytesLong123456"
        );

        properties.setAccessTokenExpiration(
            900000L
        );

        properties.setRefreshTokenExpiration(
            604800000L
        );

        jwtService =
            new JwtService(properties);

        user = new User();

        user.setUsername("admin");
        user.setEmail("admin@university.local");
        user.setPassword("encoded-password");
        user.setRole(Role.ADMIN);
        user.setEnabled(true);
    }

    @Test
    void generateAccessTokenContainsCorrectUsername() {

        String token =
            jwtService.generateAccessToken(user);

        assertNotNull(token);
        assertFalse(token.isBlank());

        assertEquals(
            "admin",
            jwtService.extractUsername(token)
        );
    }

    @Test
    void generateAccessTokenContainsCorrectRole() {

        String token =
            jwtService.generateAccessToken(user);

        assertEquals(
            "ADMIN",
            jwtService.extractRole(token)
        );
    }

    @Test
    void generatedTokenIsValid() {

        String token =
            jwtService.generateAccessToken(user);

        assertTrue(
            jwtService.isTokenValid(
                token,
                "admin"
            )
        );
    }

    @Test
    void tokenIsInvalidForDifferentUsername() {

        String token =
            jwtService.generateAccessToken(user);

        assertFalse(
            jwtService.isTokenValid(
                token,
                "different-user"
            )
        );
    }

    @Test
    void invalidTokenReturnsFalse() {

        assertFalse(
            jwtService.isTokenValid(
                "invalid.jwt.token",
                "admin"
            )
        );
    }
}