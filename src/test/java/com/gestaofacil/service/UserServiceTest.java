package com.gestaofacil.service;

import com.gestaofacil.domain.User;
import com.gestaofacil.dto.RegisterRequestDTO;
import com.gestaofacil.dto.UserResponseDTO;
import com.gestaofacil.exception.EmailAlreadyExistsException;
import com.gestaofacil.mapper.UserMapper;
import com.gestaofacil.repository.UserRepository;
import com.gestaofacil.repository.PermissionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private PermissionRepository permissionRepository;

    @Test
    void registersNormalizedEmailPhoneAndEncodedPassword() {
        UserService service = new UserService(userRepository, userMapper, passwordEncoder, permissionRepository);
        RegisterRequestDTO request = new RegisterRequestDTO(
                "Ana Souza",
                " ANA@EXAMPLE.COM ",
                "(11) 99999-1234",
                "Senha123"
        );
        UserResponseDTO expectedResponse = new UserResponseDTO(
                UUID.randomUUID(), "Ana Souza", "ana@example.com", LocalDateTime.now()
        );
        when(userRepository.existsByEmail("ana@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Senha123")).thenReturn("bcrypt-hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(permissionRepository.findCodesByUserId(any())).thenReturn(java.util.Set.of("CLIENT_VIEW"));
        when(userMapper.toResponse(any(User.class), any())).thenReturn(expectedResponse);

        UserResponseDTO response = service.register(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals("(11) 99999-1234", userCaptor.getValue().getPhone());
        assertEquals("bcrypt-hash", userCaptor.getValue().getPassword());
        assertEquals("ana@example.com", userCaptor.getValue().getEmail());
        assertEquals(expectedResponse, response);
    }

    @Test
    void rejectsDuplicateEmailBeforeSaving() {
        UserService service = new UserService(userRepository, userMapper, passwordEncoder, permissionRepository);
        RegisterRequestDTO request = new RegisterRequestDTO(
                "Ana Souza", "ana@example.com", "(11) 99999-1234", "Senha123"
        );
        when(userRepository.existsByEmail("ana@example.com")).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class, () -> service.register(request));
    }
}
