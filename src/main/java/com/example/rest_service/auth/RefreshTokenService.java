package com.example.rest_service.auth;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.http.HttpStatus;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@Transactional
public class RefreshTokenService {

    private final RefreshTokenRepository repository;
    private final JwtProperties jwtProperties;

    private final SecureRandom secureRandom =
        new SecureRandom();

    public RefreshTokenService(
            RefreshTokenRepository repository,
            JwtProperties jwtProperties) {

        this.repository = repository;
        this.jwtProperties = jwtProperties;
    }

    public RefreshToken create(User user) {

        // Remove old refresh tokens for this user.
        repository.deleteByUser(user);

        byte[] randomBytes = new byte[64];

        secureRandom.nextBytes(randomBytes);

        String token =
            Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);

        RefreshToken refreshToken =
            new RefreshToken();

        refreshToken.setToken(token);
        refreshToken.setUser(user);

        refreshToken.setExpiresAt(
            LocalDateTime.now()
                .plusSeconds(
                    jwtProperties
                        .getRefreshTokenExpiration()
                        / 1000
                )
        );

        refreshToken.setRevoked(false);

        return repository.save(refreshToken);
    }

    @Transactional(readOnly = true)
    public RefreshToken validate(String token) {

        RefreshToken refreshToken =
            repository.findByToken(token)
                .orElseThrow(() ->
                    new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Invalid refresh token"
                    )
                );

        if (refreshToken.isRevoked()) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Refresh token has been revoked"
            );
        }

        if (refreshToken.getExpiresAt()
                .isBefore(LocalDateTime.now())) {

            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Refresh token has expired"
            );
        }

        return refreshToken;
    }

    public void revoke(String token) {

        RefreshToken refreshToken =
            repository.findByToken(token)
                .orElseThrow(() ->
                    new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Invalid refresh token"
                    )
                );

        refreshToken.setRevoked(true);

        repository.save(refreshToken);
    }
}