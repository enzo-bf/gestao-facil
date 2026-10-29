package com.gestaofacil.security;

import com.gestaofacil.config.AuthCookieProperties;
import com.gestaofacil.config.JwtProperties;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class AuthCookieFactory {

    public static final String AUTH_COOKIE_NAME = "GF-AUTH";

    private final AuthCookieProperties cookieProperties;
    private final JwtProperties jwtProperties;

    public AuthCookieFactory(AuthCookieProperties cookieProperties, JwtProperties jwtProperties) {
        this.cookieProperties = cookieProperties;
        this.jwtProperties = jwtProperties;
    }

    public ResponseCookie authenticationCookie(String token) {
        return ResponseCookie.from(AUTH_COOKIE_NAME, token)
                .httpOnly(true)
                .secure(cookieProperties.secure())
                .sameSite(cookieProperties.sameSite())
                .path("/api")
                .maxAge(Duration.ofSeconds(jwtProperties.expirationSeconds()))
                .build();
    }

    public ResponseCookie clearAuthenticationCookie() {
        return ResponseCookie.from(AUTH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(cookieProperties.secure())
                .sameSite(cookieProperties.sameSite())
                .path("/api")
                .maxAge(Duration.ZERO)
                .build();
    }
}
