package com.gestaofacil.service;

import com.gestaofacil.dto.SetupStatusDTO;
import com.gestaofacil.dto.UserRequestDTO;
import com.gestaofacil.dto.UserResponseDTO;
import com.gestaofacil.exception.SetupAlreadyCompletedException;
import com.gestaofacil.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SetupServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService;

    private SetupService setupService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        setupService = new SetupService(jdbcTemplate, userRepository, userService, "initial-setup-token-for-tests-0123456789");
    }

    @Test
    void reportsFirstAccessWhenNoUsersExist() {
        when(userRepository.existsAny()).thenReturn(false);

        assertEquals(new SetupStatusDTO(true), setupService.status());
    }

    @Test
    void reportsExistingInstallationWhenAUserExists() {
        when(userRepository.existsAny()).thenReturn(true);

        assertEquals(new SetupStatusDTO(false), setupService.status());
    }

    @Test
    void createsFirstUserAfterAcquiringDatabaseLock() {
        UserRequestDTO request = new UserRequestDTO("Ana Souza", "ana@example.com", "senha-segura-123");
        UserResponseDTO expected = new UserResponseDTO(UUID.randomUUID(), "Ana Souza", "ana@example.com", LocalDateTime.now());
        when(userRepository.existsAny()).thenReturn(false);
        when(userService.register(request)).thenReturn(expected);

        assertEquals(expected, setupService.createFirstUser(request, "initial-setup-token-for-tests-0123456789"));

        verify(jdbcTemplate).execute("SELECT pg_advisory_xact_lock(71620260102)");
        verify(userService).register(request);
    }

    @Test
    void rejectsFirstUserCreationAfterSetupIsComplete() {
        UserRequestDTO request = new UserRequestDTO("Ana Souza", "ana@example.com", "senha-segura-123");
        when(userRepository.existsAny()).thenReturn(true);

        assertThrows(SetupAlreadyCompletedException.class,
                () -> setupService.createFirstUser(request, "initial-setup-token-for-tests-0123456789"));

        verify(jdbcTemplate).execute("SELECT pg_advisory_xact_lock(71620260102)");
        verify(userService, never()).register(request);
    }

    @Test
    void rejectsMissingOrInvalidSetupTokenBeforeCheckingDatabase() {
        UserRequestDTO request = new UserRequestDTO("Ana Souza", "ana@example.com", "senha-segura-123");

        assertThrows(com.gestaofacil.exception.InvalidSetupTokenException.class,
                () -> setupService.createFirstUser(request, "incorrect-token"));

        verify(userRepository, never()).existsAny();
        verify(userService, never()).register(request);
    }

    @Test
    void disablesSetupWhenNoBootstrapTokenIsConfigured() {
        SetupService disabledSetup = new SetupService(jdbcTemplate, userRepository, userService, "");
        UserRequestDTO request = new UserRequestDTO("Ana Souza", "ana@example.com", "senha-segura-123");

        assertThrows(com.gestaofacil.exception.SetupUnavailableException.class,
                () -> disabledSetup.createFirstUser(request, "any-token"));
        verify(userRepository, never()).existsAny();
    }
}
