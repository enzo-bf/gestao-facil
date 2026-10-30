package com.gestaofacil.service;

import com.gestaofacil.domain.User;
import com.gestaofacil.dto.ProfileRequestDTO;
import com.gestaofacil.exception.EmailAlreadyExistsException;
import com.gestaofacil.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    private static final String EMAIL = "ana@example.com";
    private static final UUID ID = UUID.randomUUID();

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountEmailService accountEmailService;

    private ProfileService profileService;

    @BeforeEach
    void setUp() {
        profileService = new ProfileService(userRepository, accountEmailService);
    }

    @Test
    void updatesEmailAndNormalizesBrazilianPhoneWithoutChangingName() {
        User user = new User(ID, "Ana Souza", EMAIL, null, "encoded", LocalDateTime.now());
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = profileService.update(
                EMAIL,
                new ProfileRequestDTO("ANA.NEW@example.com", "(11) 99999-0000")
        );

        assertEquals("Ana Souza", response.name());
        assertEquals("ana.new@example.com", response.email());
        assertEquals("11999990000", response.phone());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void rejectsEmailOwnedByAnotherAccount() {
        User user = new User(ID, "Ana Souza", EMAIL, null, "encoded", LocalDateTime.now());
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("other@example.com")).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class, () -> profileService.update(
                EMAIL,
                new ProfileRequestDTO("other@example.com", null)
        ));
    }
}
