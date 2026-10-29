package com.gestaofacil.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.mockito.ArgumentMatchers;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthRateLimitFilterTest {

    @Test
    void limitsAuthenticationRequestsByClientAddress() throws Exception {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        AtomicInteger redisCounter = new AtomicInteger();
        when(redis.execute(
                ArgumentMatchers.<RedisScript<Long>>any(),
                ArgumentMatchers.<List<String>>any(),
                ArgumentMatchers.<Object[]>any()
        )).thenAnswer(invocation -> (long) redisCounter.incrementAndGet());
        AuthRateLimitFilter firstInstance = new AuthRateLimitFilter(redis, 1);
        AuthRateLimitFilter secondInstance = new AuthRateLimitFilter(redis, 1);
        AtomicInteger passedRequests = new AtomicInteger();
        FilterChain chain = (request, response) -> passedRequests.incrementAndGet();

        MockHttpServletRequest firstRequest = authRequest();
        MockHttpServletResponse firstResponse = new MockHttpServletResponse();
        firstInstance.doFilter(firstRequest, firstResponse, chain);

        MockHttpServletRequest secondRequest = authRequest();
        MockHttpServletResponse secondResponse = new MockHttpServletResponse();
        secondInstance.doFilter(secondRequest, secondResponse, chain);

        assertEquals(1, passedRequests.get());
        assertEquals(429, secondResponse.getStatus());
        assertTrue(Integer.parseInt(secondResponse.getHeader("Retry-After")) >= 1);
        assertTrue(secondResponse.getContentAsString().contains("Muitas tentativas de autenticação."));
    }

    @Test
    void failsClosedWhenRedisIsUnavailable() throws Exception {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.execute(
                ArgumentMatchers.<RedisScript<Long>>any(),
                ArgumentMatchers.<List<String>>any(),
                ArgumentMatchers.<Object[]>any()
        )).thenThrow(new IllegalStateException("Redis connection failed"));
        AuthRateLimitFilter filter = new AuthRateLimitFilter(redis, 20);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicInteger passedRequests = new AtomicInteger();

        filter.doFilter(authRequest(), response, (request, result) -> passedRequests.incrementAndGet());

        assertEquals(503, response.getStatus());
        assertEquals(0, passedRequests.get());
    }

    private MockHttpServletRequest authRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setServletPath("/api/v1/auth/login");
        request.setRemoteAddr("198.51.100.24");
        return request;
    }
}
