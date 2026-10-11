
package com.example.rest_service.auth;

import com.example.rest_service.auth.dto.LoginRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
        this.refreshTokenService = refreshTokenService;
    }

    /**
     * Authenticates the user and creates a new session.
     *
     * The refresh token is returned internally to AuthController,
     * which sends it to the browser as an HttpOnly cookie.
     */
    @Transactional
    public LoginResult login(LoginRequest request) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.username(),
                        request.password()));

        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Invalid username or password"));

        if (!user.isEnabled()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "User account is disabled");
        }

        String accessToken = jwtService.generateAccessToken(user);

        RefreshToken refreshToken = refreshTokenService.create(user);

        return new LoginResult(
                accessToken,
                refreshToken.getToken(),
                "Bearer",
                jwtProperties.getAccessTokenExpiration(),
                user.getUsername(),
                user.getRole().name());
    }

    /**
     * Validates and rotates the refresh token.
     *
     * The controller returns the new refresh token in an
     * HttpOnly cookie, never in the JSON response.
     */
    @Transactional
    public RefreshResult refresh(String refreshTokenValue) {

        if (refreshTokenValue == null || refreshTokenValue.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Refresh token is missing");
        }

        RefreshToken currentToken = refreshTokenService.validate(refreshTokenValue);

        User user = currentToken.getUser();

        if (!user.isEnabled()) {
            refreshTokenService.revoke(refreshTokenValue);

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "User account is disabled");
        }

        String accessToken = jwtService.generateAccessToken(user);

        // Creates a replacement token. The current RefreshTokenService
        // deletes existing refresh tokens for this user before saving
        // the replacement.
        RefreshToken replacementToken = refreshTokenService.create(user);

        return new RefreshResult(
                accessToken,
                replacementToken.getToken(),
                "Bearer",
                jwtProperties.getAccessTokenExpiration(),
                user.getUsername(),
                user.getRole().name());
    }

    /**
     * Revokes the refresh token associated with the browser session.
     */
    @Transactional
    public void logout(String refreshTokenValue) {

        if (refreshTokenValue == null || refreshTokenValue.isBlank()) {
            return;
        }

        refreshTokenService.revoke(refreshTokenValue);
    }
}