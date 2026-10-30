package com.gestaofacil.service;

import com.gestaofacil.domain.User;
import com.gestaofacil.dto.PermissionResponseDTO;
import com.gestaofacil.exception.InvalidUserOperationException;
import com.gestaofacil.repository.PermissionRepository;
import com.gestaofacil.repository.OrganizationRepository;
import com.gestaofacil.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserManagementServiceTest {

    private final UUID organizationId = UUID.randomUUID();
    private final User actor = user(UUID.randomUUID(), "admin@example.com");
    private final User target = user(UUID.randomUUID(), "member@example.com");

    @Mock
    private UserRepository userRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AccountEmailService accountEmailService;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private UserManagementService userManagementService;

    @BeforeEach
    void setUp() {
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(actor));
        when(userRepository.findByIdAndOrganizationId(target.getId(), organizationId))
                .thenReturn(Optional.of(target));
        when(permissionRepository.findAll()).thenReturn(List.of(
                new PermissionResponseDTO("CLIENT_VIEW", "Ver clientes", "Consultar clientes.")
        ));
    }

    @Test
    void updatesPermissionsForAUserInTheSameWorkspace() {
        when(permissionRepository.findCodesByUserId(target.getId())).thenReturn(Set.of());

        userManagementService.updatePermissions("admin@example.com", target.getId(), Set.of("CLIENT_VIEW"));

        verify(permissionRepository).replace(target.getId(), Set.of("CLIENT_VIEW"));
        verify(auditService).record("admin@example.com", "UPDATE_PERMISSIONS", "USER", target.getId().toString());
    }

    @Test
    void rejectsRemovingTheLastActiveAdministratorPermission() {
        when(permissionRepository.findCodesByUserId(target.getId())).thenReturn(Set.of("USER_MANAGE"));
        when(userRepository.findAllByOrganizationId(organizationId)).thenReturn(List.of(actor, target));
        when(permissionRepository.findCodesByUserId(actor.getId())).thenReturn(Set.of());

        assertThrows(InvalidUserOperationException.class, () ->
                userManagementService.updatePermissions(
                        "admin@example.com", target.getId(), Set.of("CLIENT_VIEW")
                )
        );
    }

    private User user(UUID id, String email) {
        return new User(
                id,
                "Workspace Member",
                email,
                null,
                "encoded-password",
                LocalDateTime.now(),
                organizationId,
                true,
                true
        );
    }
}
