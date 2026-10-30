package com.gestaofacil.service;

import com.gestaofacil.domain.Budget;
import com.gestaofacil.domain.Client;
import com.gestaofacil.domain.ServiceOrder;
import com.gestaofacil.domain.User;
import com.gestaofacil.dto.BudgetRequestDTO;
import com.gestaofacil.dto.BudgetResponseDTO;
import com.gestaofacil.dto.BudgetStatusRequestDTO;
import com.gestaofacil.exception.BudgetNotFoundException;
import com.gestaofacil.exception.ClientNotFoundException;
import com.gestaofacil.exception.ServiceOrderNotFoundException;
import com.gestaofacil.mapper.BudgetMapper;
import com.gestaofacil.repository.BudgetRepository;
import com.gestaofacil.repository.ClientRepository;
import com.gestaofacil.repository.ServiceOrderRepository;
import com.gestaofacil.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final ClientRepository clientRepository;
    private final ServiceOrderRepository serviceOrderRepository;
    private final UserRepository userRepository;
    private final BudgetMapper budgetMapper;
    private final BudgetPdfService budgetPdfService;

    @Transactional
    public BudgetResponseDTO create(String authenticatedEmail, BudgetRequestDTO request) {
        UUID ownerId = findOwnerId(authenticatedEmail);
        Client client = findOwnedClient(request.clientId(), ownerId);
        validateServiceOrder(request.serviceOrderId(), request.clientId(), ownerId);

        Budget budget = Budget.create(
                budgetRepository.nextBudgetNumber(),
                ownerId,
                request.clientId(),
                request.serviceOrderId(),
                request.description().trim(),
                request.value()
        );
        return budgetMapper.toResponse(budgetRepository.save(budget), client.getName());
    }

    @Transactional(readOnly = true)
    public List<BudgetResponseDTO> findAll(String authenticatedEmail) {
        UUID ownerId = findOwnerId(authenticatedEmail);
        List<Budget> budgets = budgetRepository.findAllByOwnerId(ownerId);
        Map<UUID, String> clientNames = clientRepository.findAllByOwnerId(ownerId).stream()
                .collect(Collectors.toMap(Client::getId, Client::getName));
        return budgets.stream()
                .map(budget -> budgetMapper.toResponse(
                        budget,
                        clientNames.getOrDefault(budget.getClientId(), "Cliente")
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public BudgetResponseDTO findById(String authenticatedEmail, UUID id) {
        UUID ownerId = findOwnerId(authenticatedEmail);
        Budget budget = findOwnedBudget(id, ownerId);
        Client client = findOwnedClient(budget.getClientId(), ownerId);
        return budgetMapper.toResponse(budget, client.getName());
    }

    @Transactional
    public BudgetResponseDTO update(String authenticatedEmail, UUID id, BudgetRequestDTO request) {
        UUID ownerId = findOwnerId(authenticatedEmail);
        Client client = findOwnedClient(request.clientId(), ownerId);
        validateServiceOrder(request.serviceOrderId(), request.clientId(), ownerId);
        Budget budget = findOwnedBudget(id, ownerId).update(
                request.clientId(),
                request.serviceOrderId(),
                request.description().trim(),
                request.value()
        );
        return budgetMapper.toResponse(budgetRepository.save(budget), client.getName());
    }

    @Transactional
    public BudgetResponseDTO updateStatus(String authenticatedEmail, UUID id, BudgetStatusRequestDTO request) {
        UUID ownerId = findOwnerId(authenticatedEmail);
        Budget budget = findOwnedBudget(id, ownerId).changeStatus(request.status());
        Client client = findOwnedClient(budget.getClientId(), ownerId);
        return budgetMapper.toResponse(budgetRepository.save(budget), client.getName());
    }

    @Transactional
    public void delete(String authenticatedEmail, UUID id) {
        UUID ownerId = findOwnerId(authenticatedEmail);
        budgetRepository.delete(findOwnedBudget(id, ownerId));
    }

    @Transactional(readOnly = true)
    public byte[] generatePdf(String authenticatedEmail, UUID id) {
        UUID ownerId = findOwnerId(authenticatedEmail);
        Budget budget = findOwnedBudget(id, ownerId);
        Client client = findOwnedClient(budget.getClientId(), ownerId);
        return budgetPdfService.generate(budget, client);
    }

    private UUID findOwnerId(String authenticatedEmail) {
        User user = userRepository.findByEmail(normalizeEmail(authenticatedEmail))
                .orElseThrow(() -> new EntityNotFoundException("Usuário autenticado não encontrado."));
        return user.getOrganizationId();
    }

    private Client findOwnedClient(UUID clientId, UUID ownerId) {
        return clientRepository.findByIdAndOwnerId(clientId, ownerId)
                .orElseThrow(ClientNotFoundException::new);
    }

    private void validateServiceOrder(UUID serviceOrderId, UUID clientId, UUID ownerId) {
        if (serviceOrderId == null) return;
        ServiceOrder serviceOrder = serviceOrderRepository.findByIdAndOwnerId(serviceOrderId, ownerId)
                .orElseThrow(ServiceOrderNotFoundException::new);
        if (!serviceOrder.getClientId().equals(clientId)) {
            throw new ServiceOrderNotFoundException();
        }
    }

    private Budget findOwnedBudget(UUID id, UUID ownerId) {
        return budgetRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(BudgetNotFoundException::new);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
