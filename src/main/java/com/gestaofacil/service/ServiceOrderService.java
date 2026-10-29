package com.gestaofacil.service;

import com.gestaofacil.domain.Client;
import com.gestaofacil.domain.ServiceOrder;
import com.gestaofacil.domain.ServiceOrderStatus;
import com.gestaofacil.domain.User;
import com.gestaofacil.dto.ServiceOrderRequestDTO;
import com.gestaofacil.dto.ServiceOrderResponseDTO;
import com.gestaofacil.dto.ServiceOrderStatusRequestDTO;
import com.gestaofacil.exception.ClientNotFoundException;
import com.gestaofacil.exception.ServiceOrderNotFoundException;
import com.gestaofacil.mapper.ServiceOrderMapper;
import com.gestaofacil.repository.ClientRepository;
import com.gestaofacil.repository.ServiceOrderRepository;
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
public class ServiceOrderService {

    private final ServiceOrderRepository serviceOrderRepository;
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final ServiceOrderMapper serviceOrderMapper;

    @Transactional
    public ServiceOrderResponseDTO create(String authenticatedEmail, ServiceOrderRequestDTO request) {
        UUID ownerId = findOwnerId(authenticatedEmail);
        Client client = findOwnedClient(request.clientId(), ownerId);
        ServiceOrder serviceOrder = ServiceOrder.create(
                client.getId(),
                request.description().trim(),
                request.value()
        );
        return serviceOrderMapper.toResponse(serviceOrderRepository.save(serviceOrder));
    }

    @Transactional(readOnly = true)
    public List<ServiceOrderResponseDTO> findAll(String authenticatedEmail) {
        UUID ownerId = findOwnerId(authenticatedEmail);
        return serviceOrderRepository.findAllByOwnerId(ownerId).stream()
                .map(serviceOrderMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ServiceOrderResponseDTO findById(String authenticatedEmail, UUID id) {
        UUID ownerId = findOwnerId(authenticatedEmail);
        return serviceOrderMapper.toResponse(findOwnedServiceOrder(id, ownerId));
    }

    @Transactional
    public ServiceOrderResponseDTO update(
            String authenticatedEmail,
            UUID id,
            ServiceOrderRequestDTO request
    ) {
        UUID ownerId = findOwnerId(authenticatedEmail);
        findOwnedClient(request.clientId(), ownerId);
        ServiceOrder serviceOrder = findOwnedServiceOrder(id, ownerId).update(
                request.clientId(),
                request.description().trim(),
                request.value()
        );
        return serviceOrderMapper.toResponse(serviceOrderRepository.save(serviceOrder));
    }

    @Transactional
    public ServiceOrderResponseDTO updateStatus(
            String authenticatedEmail,
            UUID id,
            ServiceOrderStatusRequestDTO request
    ) {
        UUID ownerId = findOwnerId(authenticatedEmail);
        ServiceOrder serviceOrder = findOwnedServiceOrder(id, ownerId).changeStatus(request.status());
        return serviceOrderMapper.toResponse(serviceOrderRepository.save(serviceOrder));
    }

    @Transactional
    public void delete(String authenticatedEmail, UUID id) {
        UUID ownerId = findOwnerId(authenticatedEmail);
        serviceOrderRepository.delete(findOwnedServiceOrder(id, ownerId));
    }

    private UUID findOwnerId(String authenticatedEmail) {
        User user = userRepository.findByEmail(normalizeEmail(authenticatedEmail))
                .orElseThrow(() -> new EntityNotFoundException("Usuário autenticado não encontrado."));
        return user.getId();
    }

    private Client findOwnedClient(UUID clientId, UUID ownerId) {
        return clientRepository.findByIdAndOwnerId(clientId, ownerId)
                .orElseThrow(ClientNotFoundException::new);
    }

    private ServiceOrder findOwnedServiceOrder(UUID id, UUID ownerId) {
        return serviceOrderRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(ServiceOrderNotFoundException::new);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
