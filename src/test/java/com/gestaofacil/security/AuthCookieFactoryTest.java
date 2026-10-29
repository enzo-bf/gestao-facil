package com.gestaofacil.security;

import com.gestaofacil.config.AuthCookieProperties;
import com.gestaofacil.config.JwtProperties;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthCookieFactoryTest {

    private static final JwtProperties JWT_PROPERTIES = new JwtProperties(
            Base64.getEncoder().encodeToString("a-local-test-secret-with-at-least-32-bytes".getBytes()),
            "gestao-facil-test",
            600
    );

    @Test
    void createsHttpOnlySecureCookieWithConfiguredSameSiteAndLifetime() {
        AuthCookieFactory factory = new AuthCookieFactory(
                new AuthCookieProperties(true, "None"),
                JWT_PROPERTIES
        );

        String cookie = factory.authenticationCookie("test-token").toString();

        assertTrue(cookie.contains("HttpOnly"));
        assertTrue(cookie.contains("Secure"));
        assertTrue(cookie.contains("SameSite=None"));
        assertTrue(cookie.contains("Path=/api"));
        assertTrue(cookie.contains("Max-Age=600"));
    }

    @Test
    void rejectsSameSiteNoneWithoutSecureCookies() {
        assertThrows(IllegalArgumentException.class, () -> new AuthCookieProperties(false, "None"));
    }
}
