package com.gestaofacil.security;

import com.gestaofacil.config.JwtProperties;
import com.gestaofacil.repository.TokenRevocationRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    @Test
    void rejectsRevokedToken() throws Exception {
        String secret = Base64.getEncoder().encodeToString(
                "a-local-test-secret-with-at-least-32-bytes".getBytes()
        );
        JwtService jwtService = new JwtService(new JwtProperties(secret, "gestao-facil-test", 600));
        TokenRevocationRepository revocations = mock(TokenRevocationRepository.class);
        var entryPoint = (org.springframework.security.web.AuthenticationEntryPoint)
                (request, response, exception) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, revocations, entryPoint);
        String token = jwtService.generateToken("user@example.com");
        when(revocations.isRevoked(jwtService.extractTokenId(token))).thenReturn(true);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/clients");
        request.setCookies(new Cookie(AuthCookieFactory.AUTH_COOKIE_NAME, token));
        MockHttpServletResponse response = new MockHttpServletResponse();
        var chainReached = new java.util.concurrent.atomic.AtomicBoolean();

        filter.doFilter(request, response, (req, res) -> chainReached.set(true));

        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
        assertEquals(false, chainReached.get());
        verify(revocations).isRevoked(jwtService.extractTokenId(token));
    }

    @Test
    void doesNotAuthenticateFromAuthorizationHeader() throws Exception {
        String secret = Base64.getEncoder().encodeToString(
                "a-local-test-secret-with-at-least-32-bytes".getBytes()
        );
        JwtService jwtService = new JwtService(new JwtProperties(secret, "gestao-facil-test", 600));
        TokenRevocationRepository revocations = mock(TokenRevocationRepository.class);
        var entryPoint = (org.springframework.security.web.AuthenticationEntryPoint)
                (request, response, exception) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, revocations, entryPoint);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/clients");
        request.addHeader("Authorization", "Bearer " + jwtService.generateToken("user@example.com"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        var chainReached = new java.util.concurrent.atomic.AtomicBoolean();

        filter.doFilter(request, response, (req, res) -> chainReached.set(true));

        assertEquals(true, chainReached.get());
        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
    }

    @Test
    void expiredCookieDoesNotBlockPublicLogin() throws Exception {
        String secret = Base64.getEncoder().encodeToString(
                "a-local-test-secret-with-at-least-32-bytes".getBytes()
        );
        JwtService jwtService = new JwtService(new JwtProperties(secret, "gestao-facil-test", 600));
        TokenRevocationRepository revocations = mock(TokenRevocationRepository.class);
        var entryPoint = (org.springframework.security.web.AuthenticationEntryPoint)
                (request, response, exception) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, revocations, entryPoint);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setServletPath("/api/v1/auth/login");
        request.setCookies(new Cookie(AuthCookieFactory.AUTH_COOKIE_NAME, "stale-token"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        var chainReached = new java.util.concurrent.atomic.AtomicBoolean();

        filter.doFilter(request, response, (req, res) -> chainReached.set(true));

        assertEquals(true, chainReached.get());
        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
    }
}
