package com.example.rest_service.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private final JwtProperties jwtProperties;
    private final SecretKey signingKey;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;

        this.signingKey = Keys.hmacShaKeyFor(
            jwtProperties.getSecret()
                .getBytes(StandardCharsets.UTF_8)
        );
    }

    public String generateAccessToken(User user) {

        Date issuedAt = new Date();

        Date expiration = new Date(
            issuedAt.getTime()
                + jwtProperties.getAccessTokenExpiration()
        );

        return Jwts.builder()
            .subject(user.getUsername())
            .claim("role", user.getRole().name())
            .issuedAt(issuedAt)
            .expiration(expiration)
            .signWith(signingKey)
            .compact();
    }

    public String extractUsername(String token) {

        return extractAllClaims(token)
            .getSubject();
    }

    public String extractRole(String token) {

        return extractAllClaims(token)
            .get("role", String.class);
    }

    public boolean isTokenValid(
            String token,
            String username) {

        try {

            String tokenUsername =
                extractUsername(token);

            return tokenUsername.equals(username)
                && !isTokenExpired(token);

        } catch (Exception ex) {
            return false;
        }
    }

    private boolean isTokenExpired(String token) {

        Date expiration =
            extractAllClaims(token).getExpiration();

        return expiration.before(new Date());
    }

    private Claims extractAllClaims(String token) {

        return Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}