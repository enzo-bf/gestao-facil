package com.gestaofacil.service;

import com.gestaofacil.domain.User;
import com.gestaofacil.dto.CreateManagedUserRequestDTO;
import com.gestaofacil.dto.ManagedUserResponseDTO;
import com.gestaofacil.dto.PermissionResponseDTO;
import com.gestaofacil.exception.EmailAlreadyExistsException;
import com.gestaofacil.exception.InvalidUserOperationException;
import com.gestaofacil.repository.PermissionRepository;
import com.gestaofacil.repository.OrganizationRepository;
import com.gestaofacil.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserManagementService {

    private static final String USER_MANAGE = "USER_MANAGE";
    private final UserRepository userRepository;
    private final PermissionRepository permissionRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccountEmailService accountEmailService;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<ManagedUserResponseDTO> findAll(String actorEmail) {
        User actor = findActor(actorEmail);
        return userRepository.findAllByOrganizationId(actor.getOrganizationId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PermissionResponseDTO> permissions() {
        return permissionRepository.findAll();
    }

    @Transactional
    public ManagedUserResponseDTO invite(String actorEmail, CreateManagedUserRequestDTO request) {
        User actor = findActor(actorEmail);
        organizationRepository.lockForUpdate(actor.getOrganizationId());
        String email = UserService.normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) throw new EmailAlreadyExistsException();
        Set<String> selected = validatePermissions(request.permissions());
        UUID userId = UUID.randomUUID();
        String randomPassword = UUID.randomUUID().toString();
        User invited = User.invite(
                userId,
                actor.getOrganizationId(),
                request.name().trim(),
                email,
                passwordEncoder.encode(randomPassword)
        );
        User saved = userRepository.save(invited);
        permissionRepository.replace(saved.getId(), selected);
        accountEmailService.sendInvitation(saved);
        auditService.record(actorEmail, "INVITE", "USER", saved.getId().toString());
        return toResponse(saved);
    }

    @Transactional
    public ManagedUserResponseDTO updatePermissions(String actorEmail, UUID userId, Set<String> requested) {
        User actor = findActor(actorEmail);
        organizationRepository.lockForUpdate(actor.getOrganizationId());
        User target = findTarget(userId, actor.getOrganizationId());
        Set<String> selected = validatePermissions(requested);
        Set<String> current = permissionRepository.findCodesByUserId(target.getId());
        if (current.contains(USER_MANAGE) && !selected.contains(USER_MANAGE)) {
            ensureAnotherActiveManager(actor.getOrganizationId(), target.getId());
        }
        permissionRepository.replace(target.getId(), selected);
        auditService.record(actorEmail, "UPDATE_PERMISSIONS", "USER", target.getId().toString());
        return toResponse(target);
    }

    @Transactional
    public ManagedUserResponseDTO updateStatus(String actorEmail, UUID userId, boolean active) {
        User actor = findActor(actorEmail);
        organizationRepository.lockForUpdate(actor.getOrganizationId());
        User target = findTarget(userId, actor.getOrganizationId());
        if (actor.getId().equals(target.getId()) && !active) {
            throw new InvalidUserOperationException("Você não pode desativar sua própria conta.");
        }
        if (!active && permissionRepository.findCodesByUserId(target.getId()).contains(USER_MANAGE)) {
            ensureAnotherActiveManager(actor.getOrganizationId(), target.getId());
        }
        User updated = userRepository.save(target.withActive(active));
        auditService.record(actorEmail, active ? "ACTIVATE" : "DEACTIVATE", "USER", target.getId().toString());
        return toResponse(updated);
    }

    @Transactional
    public void remove(String actorEmail, UUID userId) {
        User actor = findActor(actorEmail);
        organizationRepository.lockForUpdate(actor.getOrganizationId());
        User target = findTarget(userId, actor.getOrganizationId());
        if (actor.getId().equals(target.getId())) {
            throw new InvalidUserOperationException("Você não pode remover sua própria conta.");
        }
        if (permissionRepository.findCodesByUserId(target.getId()).contains(USER_MANAGE)) {
            ensureAnotherActiveManager(actor.getOrganizationId(), target.getId());
        }
        auditService.record(actorEmail, "REMOVE", "USER", target.getId().toString());
        userRepository.delete(target);
    }

    private Set<String> validatePermissions(Set<String> requested) {
        Set<String> valid = permissionRepository.findAll().stream()
                .map(PermissionResponseDTO::code).collect(Collectors.toSet());
        if (!valid.containsAll(requested)) {
            throw new IllegalArgumentException("Uma ou mais permissões informadas não são válidas.");
        }
        return Set.copyOf(requested);
    }

    private void ensureAnotherActiveManager(UUID organizationId, UUID excludedUserId) {
        boolean anotherManagerExists = userRepository.findAllByOrganizationId(organizationId).stream()
                .filter(user -> !user.getId().equals(excludedUserId))
                .filter(User::isActive)
                .filter(User::isEmailVerified)
                .anyMatch(user -> permissionRepository.findCodesByUserId(user.getId()).contains(USER_MANAGE));
        if (!anotherManagerExists) {
            throw new InvalidUserOperationException("O workspace precisa manter ao menos um administrador ativo.");
        }
    }

    private ManagedUserResponseDTO toResponse(User user) {
        Set<String> codes = permissionRepository.findCodesByUserId(user.getId());
        return new ManagedUserResponseDTO(
                user.getId(), user.getName(), user.getEmail(), user.isActive(),
                user.isEmailVerified(), codes.size(), codes
        );
    }

    private User findActor(String email) {
        return userRepository.findByEmail(normalize(email))
                .orElseThrow(() -> new EntityNotFoundException("Usuário autenticado não encontrado."));
    }

    private User findTarget(UUID userId, UUID organizationId) {
        return userRepository.findByIdAndOrganizationId(userId, organizationId)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado neste workspace."));
    }

    private String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
