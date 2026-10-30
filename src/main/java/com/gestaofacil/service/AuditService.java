package com.gestaofacil.service;

import com.gestaofacil.dto.AuditEventResponseDTO;
import com.gestaofacil.domain.User;
import com.gestaofacil.repository.AuditEventRepository;
import com.gestaofacil.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditEventRepository auditEventRepository;
    private final UserRepository userRepository;

    @Transactional
    public void record(String email, String action, String resource, String resourceId) {
        User actor = userRepository.findByEmail(normalize(email))
                .orElseThrow(() -> new EntityNotFoundException("Usuário autenticado não encontrado."));
        auditEventRepository.save(actor.getOrganizationId(), actor.getId(), action, resource, resourceId);
    }

    @Transactional(readOnly = true)
    public List<AuditEventResponseDTO> latest(String email, int limit) {
        User actor = userRepository.findByEmail(normalize(email))
                .orElseThrow(() -> new EntityNotFoundException("Usuário autenticado não encontrado."));
        return auditEventRepository.findLatestByOrganizationId(actor.getOrganizationId(), limit);
    }

    private String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
