package com.gestaofacil.security;

import com.gestaofacil.config.JwtProperties;
import com.gestaofacil.config.AuthCookieProperties;
import com.gestaofacil.config.SecurityConfig;
import com.gestaofacil.controller.ClientController;
import com.gestaofacil.repository.TokenRevocationRepository;
import com.gestaofacil.service.ClientService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Base64;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.mockito.Mockito.mock;

@WebMvcTest(
        controllers = ClientController.class,
        properties = "app.cors.allowed-origins=http://localhost:5173"
)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        AuthRateLimitFilter.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class,
        SecurityAuthorizationTest.TestBeans.class
})
class SecurityAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClientService clientService;

    @Test
    void deniesProtectedResourceWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/clients"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deniesProtectedResourceWithInvalidToken() throws Exception {
        mockMvc.perform(get("/api/v1/clients").header("Authorization", "Bearer invalid.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void requiresCsrfForStateChangingRequests() throws Exception {
        mockMvc.perform(post("/api/v1/clients"))
                .andExpect(status().isForbidden());
    }

    @Test
    void acceptsCsrfTokenBeforeAuthenticationChecks() throws Exception {
        mockMvc.perform(post("/api/v1/clients").with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @TestConfiguration
    @EnableWebSecurity
    static class TestBeans {

        @Bean
        JwtProperties jwtProperties() {
            return new JwtProperties(
                    Base64.getEncoder().encodeToString(
                            "a-local-test-secret-with-at-least-32-bytes".getBytes()
                    ),
                    "gestao-facil-test",
                    600
            );
        }

        @Bean
        AuthCookieProperties authCookieProperties() {
            return new AuthCookieProperties(false, "Lax");
        }

        @Bean
        JwtService jwtService(JwtProperties jwtProperties) {
            return new JwtService(jwtProperties);
        }

        @Bean
        TokenRevocationRepository tokenRevocationRepository() {
            return mock(TokenRevocationRepository.class);
        }

        @Bean
        StringRedisTemplate stringRedisTemplate() {
            return mock(StringRedisTemplate.class);
        }

        @Bean
        UserDetailsService userDetailsService() {
            return username -> null;
        }
    }
}
