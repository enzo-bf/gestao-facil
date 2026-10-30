package com.gestaofacil.service;

import com.gestaofacil.domain.User;
import com.gestaofacil.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountEmailServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountActionTokenService tokenService;

    @Mock
    private ResendEmailService emailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AccountEmailService accountEmailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(accountEmailService, "frontendBaseUrl", "https://app.example");
    }

    @Test
    void verifiesEmailOnlyAfterConsumingTheVerificationToken() {
        UUID userId = UUID.randomUUID();
        User user = user(userId, false, true);
        when(tokenService.consume("verification-token", "VERIFY_EMAIL")).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        accountEmailService.verify("verification-token");

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertTrue(saved.getValue().isEmailVerified());
    }

    @Test
    void passwordResetEncodesTheNewPasswordAndInvalidatesExistingSessions() {
        UUID userId = UUID.randomUUID();
        User user = user(userId, true, true);
        when(tokenService.consume("reset-token", "RESET_PASSWORD")).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("NewPassword1")).thenReturn("encoded-new-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        accountEmailService.resetPassword("reset-token", "NewPassword1");

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertEquals("encoded-new-password", saved.getValue().getPassword());
        assertEquals(1, saved.getValue().getCredentialVersion());
        verify(tokenService).invalidateForUser(userId, "RESET_PASSWORD");
    }

    @Test
    void unknownEmailDoesNotIssueOrSendARecoveryMessage() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        accountEmailService.forgotPassword("unknown@example.com");

        verifyNoInteractions(tokenService, emailService);
    }

    @Test
    void invitationEmailContainsTheTemporaryActivationLink() {
        UUID userId = UUID.randomUUID();
        User invited = User.invite(
                userId,
                UUID.randomUUID(),
                "Workspace Member",
                "member@example.com",
                "encoded-random-password"
        );
        when(tokenService.issue(any(), org.mockito.ArgumentMatchers.eq("INVITE"), any()))
                .thenReturn("one-time-token");

        accountEmailService.sendInvitation(invited);

        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(emailService).send(
                org.mockito.ArgumentMatchers.eq("member@example.com"),
                org.mockito.ArgumentMatchers.eq("Você foi convidado para o Gestão Fácil"),
                message.capture()
        );
        assertTrue(message.getValue().contains("https://app.example/activate-invitation?token=one-time-token"));
        assertTrue(message.getValue().contains("7 dias"));
        assertFalse(invited.isActive());
    }

    private User user(UUID id, boolean emailVerified, boolean active) {
        return new User(
                id,
                "Workspace Member",
                "member@example.com",
                null,
                "encoded-password",
                LocalDateTime.now(),
                UUID.randomUUID(),
                emailVerified,
                active,
                0
        );
    }
}
