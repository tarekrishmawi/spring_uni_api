
package com.example.rest_service.auth;

import com.example.rest_service.auth.dto.LoginRequest;
import tools.jackson.databind.ObjectMapper;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private JwtProperties jwtProperties;

    @Test
    void loginReturnsAccessTokenAndSetsHttpOnlyRefreshCookie()
            throws Exception {

        LoginRequest request = new LoginRequest(
                "admin",
                "Admin123!"
        );

        LoginResult result = new LoginResult(
                "test-access-token",
                "test-refresh-token",
                "Bearer",
                900000L,
                "admin",
                "ADMIN"
        );

        when(authService.login(any(LoginRequest.class)))
                .thenReturn(result);

        when(jwtProperties.getRefreshTokenExpiration())
                .thenReturn(604800000L);

        mockMvc.perform(
                post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken")
                .value("test-access-token"))
        .andExpect(jsonPath("$.tokenType")
                .value("Bearer"))
        .andExpect(jsonPath("$.expiresIn")
                .value(900000))
        .andExpect(jsonPath("$.username")
                .value("admin"))
        .andExpect(jsonPath("$.role")
                .value("ADMIN"))
        // Refresh tokens must not be exposed in JSON.
        .andExpect(jsonPath("$.refreshToken").doesNotExist())
        // The browser receives the refresh token as a cookie.
        .andExpect(header().string(
                HttpHeaders.SET_COOKIE,
                org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString(
                                "refreshToken=test-refresh-token"
                        ),
                        org.hamcrest.Matchers.containsString("HttpOnly"),
                        org.hamcrest.Matchers.containsString("SameSite=Lax"),
                        org.hamcrest.Matchers.containsString("Path=/api/auth"),
                        org.hamcrest.Matchers.containsString("Max-Age=604800")
                )
        ));
    }

    @Test
    void loginRejectsBlankUsername() throws Exception {

        LoginRequest request = new LoginRequest(
                "",
                "Admin123!"
        );

        mockMvc.perform(
                post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }

    @Test
    void loginRejectsBlankPassword() throws Exception {

        LoginRequest request = new LoginRequest(
                "admin",
                ""
        );

        mockMvc.perform(
                post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }

    @Test
    void refreshReturnsNewAccessTokenAndRotatesRefreshCookie()
            throws Exception {

        RefreshResult result = new RefreshResult(
                "new-access-token",
                "new-refresh-token",
                "Bearer",
                900000L,
                "admin",
                "ADMIN"
        );

        when(authService.refresh("old-refresh-token"))
                .thenReturn(result);

        when(jwtProperties.getRefreshTokenExpiration())
                .thenReturn(604800000L);

        mockMvc.perform(
                post("/api/auth/refresh")
                        .cookie(new Cookie(
                                "refreshToken",
                                "old-refresh-token"
                        ))
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken")
                .value("new-access-token"))
        .andExpect(jsonPath("$.username")
                .value("admin"))
        .andExpect(jsonPath("$.role")
                .value("ADMIN"))
        .andExpect(jsonPath("$.refreshToken").doesNotExist())
        .andExpect(header().string(
                HttpHeaders.SET_COOKIE,
                org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString(
                                "refreshToken=new-refresh-token"
                        ),
                        org.hamcrest.Matchers.containsString("HttpOnly"),
                        org.hamcrest.Matchers.containsString("SameSite=Lax")
                )
        ));

        verify(authService).refresh("old-refresh-token");
    }

    @Test
    void refreshRejectsMissingRefreshCookie() throws Exception {

        mockMvc.perform(post("/api/auth/refresh"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(authService);
    }

    @Test
    void logoutRevokesSessionAndClearsCookie() throws Exception {

        mockMvc.perform(
                post("/api/auth/logout")
                        .cookie(new Cookie(
                                "refreshToken",
                                "test-refresh-token"
                        ))
        )
        .andExpect(status().isNoContent())
        .andExpect(header().string(
                HttpHeaders.SET_COOKIE,
                org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("refreshToken="),
                        org.hamcrest.Matchers.containsString("Max-Age=0"),
                        org.hamcrest.Matchers.containsString("HttpOnly"),
                        org.hamcrest.Matchers.containsString("Path=/api/auth")
                )
        ));

        verify(authService).logout("test-refresh-token");
    }

    @Test
    void logoutWithoutCookieStillClearsCookie() throws Exception {

        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isNoContent())
                .andExpect(header().string(
                        HttpHeaders.SET_COOKIE,
                        org.hamcrest.Matchers.containsString("Max-Age=0")
                ));

        verify(authService).logout(eq(null));
    }
}
