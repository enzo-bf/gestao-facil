package com.gestaofacil.service;

import com.gestaofacil.domain.Client;
import com.gestaofacil.domain.User;
import com.gestaofacil.dto.ClientRequestDTO;
import com.gestaofacil.dto.ClientResponseDTO;
import com.gestaofacil.exception.ClientNotFoundException;
import com.gestaofacil.mapper.ClientMapper;
import com.gestaofacil.repository.ClientRepository;
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
public class ClientService {

    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final ClientMapper clientMapper;

    @Transactional
    public ClientResponseDTO create(String authenticatedEmail, ClientRequestDTO request) {
        UUID ownerId = findOwnerId(authenticatedEmail);
        Client client = Client.create(
                ownerId,
                request.name().trim(),
                request.phone().trim(),
                normalizeOptionalEmail(request.email()),
                normalizeOptionalValue(request.document())
        );
        return clientMapper.toResponse(clientRepository.save(client));
    }

    @Transactional
    public ClientResponseDTO update(String authenticatedEmail, UUID id, ClientRequestDTO request) {
        UUID ownerId = findOwnerId(authenticatedEmail);
        Client client = findOwnedClient(id, ownerId).update(
                request.name().trim(),
                request.phone().trim(),
                normalizeOptionalEmail(request.email()),
                normalizeOptionalValue(request.document())
        );
        return clientMapper.toResponse(clientRepository.save(client));
    }

    @Transactional(readOnly = true)
    public ClientResponseDTO findById(String authenticatedEmail, UUID id) {
        UUID ownerId = findOwnerId(authenticatedEmail);
        return clientMapper.toResponse(findOwnedClient(id, ownerId));
    }

    @Transactional(readOnly = true)
    public List<ClientResponseDTO> findAll(String authenticatedEmail) {
        UUID ownerId = findOwnerId(authenticatedEmail);
        return clientRepository.findAllByOwnerId(ownerId).stream()
                .map(clientMapper::toResponse)
                .toList();
    }

    @Transactional
    public void delete(String authenticatedEmail, UUID id) {
        UUID ownerId = findOwnerId(authenticatedEmail);
        clientRepository.delete(findOwnedClient(id, ownerId));
    }

    private UUID findOwnerId(String authenticatedEmail) {
        User user = userRepository.findByEmail(normalizeEmail(authenticatedEmail))
                .orElseThrow(() -> new EntityNotFoundException("Usuário autenticado não encontrado."));
        return user.getId();
    }

    private Client findOwnedClient(UUID id, UUID ownerId) {
        return clientRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(ClientNotFoundException::new);
    }

    private String normalizeOptionalEmail(String email) {
        String normalized = normalizeOptionalValue(email);
        return normalized == null ? null : normalizeEmail(normalized);
    }

    private String normalizeOptionalValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
