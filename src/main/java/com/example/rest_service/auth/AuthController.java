package com.example.rest_service.auth;

import com.example.rest_service.auth.dto.LoginRequest;
import com.example.rest_service.auth.dto.LoginResponse;
import com.example.rest_service.auth.dto.RefreshTokenResponse;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtProperties jwtProperties;

    @Value("${app.cookie.secure:false}")
    private boolean secureCookie;

    public AuthController(
        AuthService authService,
        JwtProperties jwtProperties
    ) {
        this.authService = authService;
        this.jwtProperties = jwtProperties;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
        @Valid @RequestBody LoginRequest request
    ) {
        LoginResult result = authService.login(request);

        ResponseCookie cookie = createRefreshCookie(result.refreshToken());

        LoginResponse body = new LoginResponse(
            result.accessToken(),
            result.tokenType(),
            result.expiresIn(),
            result.username(),
            result.role()
        );

        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, cookie.toString())
            .body(body);
    }

    @PostMapping("/refresh")
    public ResponseEntity<RefreshTokenResponse> refresh(
        @CookieValue(
            name = "refreshToken",
            required = false
        ) String refreshToken
    ) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        RefreshResult result = authService.refresh(refreshToken);

        ResponseCookie cookie = createRefreshCookie(result.refreshToken());

        RefreshTokenResponse body = new RefreshTokenResponse(
            result.accessToken(),
            result.tokenType(),
            result.expiresIn(),
            result.username(),
            result.role()
        );

        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, cookie.toString())
            .body(body);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
        @CookieValue(
            name = "refreshToken",
            required = false
        ) String refreshToken
    ) {
        authService.logout(refreshToken);

        return ResponseEntity.noContent()
            .header(
                HttpHeaders.SET_COOKIE,
                clearRefreshCookie().toString()
            )
            .build();
    }

    private ResponseCookie createRefreshCookie(String token) {
        return ResponseCookie.from("refreshToken", token)
            .httpOnly(true)
            .secure(secureCookie)
            .sameSite("Lax")
            .path("/api/auth")
            .maxAge(Duration.ofMillis(
                jwtProperties.getRefreshTokenExpiration()
            ))
            .build();
    }

    private ResponseCookie clearRefreshCookie() {
        return ResponseCookie.from("refreshToken", "")
            .httpOnly(true)
            .secure(secureCookie)
            .sameSite("Lax")
            .path("/api/auth")
            .maxAge(Duration.ZERO)
            .build();
    }
}