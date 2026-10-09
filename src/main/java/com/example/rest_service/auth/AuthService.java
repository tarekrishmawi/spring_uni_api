package com.example.rest_service.auth;

import com.example.rest_service.auth.dto.LoginRequest;
import com.example.rest_service.auth.dto.LoginResponse;
import com.example.rest_service.auth.dto.RefreshTokenResponse;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final RefreshTokenService refreshTokenService;

    public AuthService(
            AuthenticationManager authenticationManager,
            UserRepository userRepository,
            JwtService jwtService,
            JwtProperties jwtProperties,
            RefreshTokenService refreshTokenService) {

        this.authenticationManager =
            authenticationManager;

        this.userRepository =
            userRepository;

        this.jwtService =
            jwtService;

        this.jwtProperties =
            jwtProperties;

        this.refreshTokenService =
            refreshTokenService;
    }

    public LoginResponse login(LoginRequest request) {

        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                request.username(),
                request.password()
            )
        );

        User user =
            userRepository
                .findByUsername(request.username())
                .orElseThrow(() ->
                    new IllegalStateException(
                        "Authenticated user no longer exists"
                    )
                );

        String accessToken =
            jwtService.generateAccessToken(user);

        RefreshToken refreshToken =
            refreshTokenService.create(user);

        return new LoginResponse(
            accessToken,
            refreshToken.getToken(),
            "Bearer",
            jwtProperties.getAccessTokenExpiration(),
            user.getUsername(),
            user.getRole().name()
        );
    }

    public RefreshTokenResponse refresh(
            String refreshTokenValue) {

        RefreshToken refreshToken =
            refreshTokenService.validate(
                refreshTokenValue
            );

        User user =
            refreshToken.getUser();

        String accessToken =
            jwtService.generateAccessToken(user);

        return new RefreshTokenResponse(
            accessToken,
            "Bearer",
            jwtProperties.getAccessTokenExpiration(),
            user.getUsername(),
            user.getRole().name()
        );
    }

    public void logout(String refreshToken) {

        refreshTokenService.revoke(
            refreshToken
        );
    }
}