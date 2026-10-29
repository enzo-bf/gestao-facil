package com.gestaofacil.security;

import com.gestaofacil.repository.TokenRevocationRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpMethod;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final TokenRevocationRepository tokenRevocationRepository;
    private final AuthenticationEntryPoint authenticationEntryPoint;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return (HttpMethod.POST.matches(request.getMethod())
                    && ("/api/v1/auth/login".equals(path) || "/api/v1/setup/first-user".equals(path)))
                || (HttpMethod.GET.matches(request.getMethod())
                    && ("/api/v1/auth/csrf".equals(path) || "/api/v1/setup/status".equals(path)));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String token = request.getCookies() == null ? null : Arrays.stream(request.getCookies())
                .filter(cookie -> AuthCookieFactory.AUTH_COOKIE_NAME.equals(cookie.getName()))
                .map(jakarta.servlet.http.Cookie::getValue)
                .findFirst()
                .orElse(null);
        if (token == null || token.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String subject = jwtService.extractSubject(token);
            String tokenId = jwtService.extractTokenId(token);
            if (tokenRevocationRepository.isRevoked(tokenId)) {
                throw new JwtException("O token foi revogado.");
            }
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            subject,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_USER"))
                    );
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (JwtException | IllegalArgumentException exception) {
            SecurityContextHolder.clearContext();
            authenticationEntryPoint.commence(
                    request,
                    response,
                    new org.springframework.security.authentication.BadCredentialsException("Token JWT inválido.", exception)
            );
            return;
        }

        filterChain.doFilter(request, response);
    }
}
