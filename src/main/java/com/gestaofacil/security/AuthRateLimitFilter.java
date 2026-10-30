package com.gestaofacil.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final long WINDOW_SECONDS = Duration.ofMinutes(1).toSeconds();
    private static final String KEY_PREFIX = "gestao-facil:auth-rate-limit:";
    private static final DefaultRedisScript<Long> INCREMENT_SCRIPT = new DefaultRedisScript<>("""
            local count = redis.call('INCR', KEYS[1])
            if count == 1 then
                redis.call('EXPIRE', KEYS[1], ARGV[1])
            end
            return count
            """, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final int requestsPerMinute;

    public AuthRateLimitFilter(
            StringRedisTemplate redisTemplate,
            @Value("${app.security.rate-limit.auth.requests-per-minute:20}") int requestsPerMinute
    ) {
        this.redisTemplate = redisTemplate;
        if (requestsPerMinute < 1) {
            throw new IllegalArgumentException("O limite de requisições de autenticação deve ser maior que zero.");
        }
        this.requestsPerMinute = requestsPerMinute;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!HttpMethod.POST.matches(request.getMethod())) return true;
        String path = request.getServletPath();
        return !("/api/v1/auth/login".equals(path)
                || "/api/v1/auth/register".equals(path)
                || "/api/v1/auth/verify-email".equals(path)
                || "/api/v1/auth/resend-verification".equals(path)
                || "/api/v1/auth/forgot-password".equals(path)
                || "/api/v1/auth/reset-password".equals(path)
                || "/api/v1/auth/activate-invitation".equals(path))
                && !"/api/v1/setup/first-user".equals(path);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Long requestCount;
        try {
            String clientAddress = request.getRemoteAddr();
            requestCount = redisTemplate.execute(
                    INCREMENT_SCRIPT,
                    List.of(KEY_PREFIX + clientAddress),
                    Long.toString(WINDOW_SECONDS)
            );
            if (requestCount == null) {
                throw new IllegalStateException("Redis did not return an authentication rate-limit count.");
            }
        } catch (RuntimeException exception) {
            writeError(response, 503, "O serviço de autenticação está temporariamente indisponível.",
                    request.getServletPath());
            return;
        }

        if (requestCount > requestsPerMinute) {
            response.setHeader("Retry-After", Long.toString(WINDOW_SECONDS));
            writeError(response, 429, "Muitas tentativas de autenticação. Aguarde antes de tentar novamente.",
                    request.getServletPath());
            return;
        }
        filterChain.doFilter(request, response);
    }

    private void writeError(HttpServletResponse response, int status, String message, String path) throws IOException {
        String error = status == 429 ? "Too Many Requests" : "Service Unavailable";
        ApiErrorResponseWriter.write(response, status, error, message, path);
    }
}
