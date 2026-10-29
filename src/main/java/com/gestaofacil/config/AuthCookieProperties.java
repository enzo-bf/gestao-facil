package com.gestaofacil.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Set;

@ConfigurationProperties(prefix = "app.jwt.cookie")
public record AuthCookieProperties(boolean secure, String sameSite) {

    public AuthCookieProperties {
        if (sameSite == null || !Set.of("Strict", "Lax", "None").contains(sameSite)) {
            throw new IllegalArgumentException("JWT cookie SameSite must be Strict, Lax, or None.");
        }
        if ("None".equals(sameSite) && !secure) {
            throw new IllegalArgumentException("SameSite=None requires secure JWT cookies.");
        }
    }
}
