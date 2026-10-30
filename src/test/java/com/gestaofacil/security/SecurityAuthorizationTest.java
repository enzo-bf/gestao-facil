package com.gestaofacil.security;

import com.gestaofacil.config.JwtProperties;
import com.gestaofacil.config.AuthCookieProperties;
import com.gestaofacil.config.SecurityConfig;
import com.gestaofacil.controller.ClientController;
import com.gestaofacil.controller.AuthController;
import com.gestaofacil.controller.UserManagementController;
import com.gestaofacil.dto.LoginResponseDTO;
import com.gestaofacil.dto.UserResponseDTO;
import com.gestaofacil.dto.PermissionResponseDTO;
import com.gestaofacil.security.AuthCookieFactory;
import com.gestaofacil.service.AuthSession;
import com.gestaofacil.service.AuthService;
import com.gestaofacil.service.AccountEmailService;
import com.gestaofacil.repository.TokenRevocationRepository;
import com.gestaofacil.service.ClientService;
import com.gestaofacil.service.UserManagementService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.web.servlet.MockMvc;
import jakarta.servlet.http.Cookie;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.ResponseCookie;
import org.mockito.ArgumentMatchers;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.mockito.Mockito.mock;

@WebMvcTest(
        controllers = {ClientController.class, AuthController.class, UserManagementController.class},
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

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private UserManagementService userManagementService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private AuthCookieFactory authCookieFactory;

    @MockitoBean
    private AccountEmailService accountEmailService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private JwtService jwtService;

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
                .andExpect(status().isForbidden())
                .andExpect(header().string("X-CSRF-Error", "true"));
    }

    @Test
    void acceptsCsrfTokenBeforeAuthenticationChecks() throws Exception {
        mockMvc.perform(post("/api/v1/clients").with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void allowsEndpointWhenRequiredPermissionIsPresent() throws Exception {
        stubUser("client-view", "CLIENT_VIEW");
        when(clientService.findAll("client-view")).thenReturn(List.of());
        mockMvc.perform(get("/api/v1/clients")
                        .cookie(new Cookie(
                                AuthCookieFactory.AUTH_COOKIE_NAME,
                                jwtService.generateToken("client-view")
                        )))
                .andExpect(status().isOk());
    }

    @Test
    void deniesEndpointWhenUserHasDifferentPermission() throws Exception {
        stubUser("client-create-only", "CLIENT_CREATE");
        mockMvc.perform(get("/api/v1/clients")
                        .cookie(new Cookie(
                                AuthCookieFactory.AUTH_COOKIE_NAME,
                                jwtService.generateToken("client-create-only")
                        )))
                .andExpect(status().isForbidden());
    }

    @Test
    void allowsWorkspaceManagementOnlyWithUserManagePermission() throws Exception {
        stubUser("manager", "USER_MANAGE");
        when(userManagementService.permissions()).thenReturn(List.of(
                new PermissionResponseDTO("CLIENT_VIEW", "Ver clientes", "Consultar clientes.")
        ));

        mockMvc.perform(get("/api/v1/permissions")
                        .cookie(new Cookie(
                                AuthCookieFactory.AUTH_COOKIE_NAME,
                                jwtService.generateToken("manager")
                        )))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .jsonPath("$[0].code").value("CLIENT_VIEW"));
    }

    @Test
    void deniesWorkspaceManagementWithoutUserManagePermission() throws Exception {
        stubUser("client-view", "CLIENT_VIEW");
        mockMvc.perform(get("/api/v1/permissions")
                        .cookie(new Cookie(
                                AuthCookieFactory.AUTH_COOKIE_NAME,
                                jwtService.generateToken("client-view")
                        )))
                .andExpect(status().isForbidden());
    }

    private void stubUser(String username, String permission) {
        when(userDetailsService.loadUserByUsername(username)).thenReturn(
                new SessionUserDetails(username, "unused", true, 0,
                        List.of(new SimpleGrantedAuthority(permission)))
        );
    }

    @Test
    void registrationIsPublicButRequiresCsrf() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content("""
                                {"name":"Ana Souza","email":"ana@example.com","phone":"(11) 99999-1234","password":"Senha123"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(header().string("X-CSRF-Error", "true"));
    }

    @Test
    void emailVerificationRequiresCsrfAndDelegatesTokenConsumption() throws Exception {
        when(redisTemplate.execute(
                ArgumentMatchers.<RedisScript<Long>>any(),
                ArgumentMatchers.<List<String>>any(),
                ArgumentMatchers.<Object[]>any()
        )).thenReturn(1L);

        mockMvc.perform(post("/api/v1/auth/verify-email")
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"token\":\"one-time-token\"}"))
                .andExpect(status().isOk());

        verify(accountEmailService).verify("one-time-token");
    }

    @Test
    void createsPublicRegistrationSessionWithHttpOnlyCookieAfterCsrf() throws Exception {
        UserResponseDTO user = new UserResponseDTO(
                UUID.randomUUID(), "Ana Souza", "ana@example.com", LocalDateTime.now()
        );
        when(redisTemplate.execute(
                ArgumentMatchers.<RedisScript<Long>>any(),
                ArgumentMatchers.<List<String>>any(),
                ArgumentMatchers.<Object[]>any()
        )).thenReturn(1L);
        when(authService.register(ArgumentMatchers.any())).thenReturn(
                new AuthSession("signed-jwt", new LoginResponseDTO(600, user))
        );
        when(authCookieFactory.authenticationCookie("signed-jwt")).thenReturn(
                ResponseCookie.from(AuthCookieFactory.AUTH_COOKIE_NAME, "signed-jwt")
                        .httpOnly(true)
                        .path("/api")
                        .build()
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {"name":"Ana Souza","email":"ana@example.com","phone":"(11) 99999-1234","password":"Senha123"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Set-Cookie",
                        org.hamcrest.Matchers.containsString("GF-AUTH=signed-jwt")))
                .andExpect(header().string("Set-Cookie",
                        org.hamcrest.Matchers.containsString("HttpOnly")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .jsonPath("$.user.email").value("ana@example.com"));
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

    }
}
