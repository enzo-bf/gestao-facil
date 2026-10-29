package com.gestaofacil.service;

import com.gestaofacil.domain.Budget;
import com.gestaofacil.domain.BudgetStatus;
import com.gestaofacil.domain.Client;
import com.gestaofacil.domain.ServiceOrder;
import com.gestaofacil.domain.ServiceOrderStatus;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    private static final String EMAIL = "ana@example.com";
    private static final UUID OWNER_ID = UUID.randomUUID();

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private ServiceOrderRepository serviceOrderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BudgetPdfService budgetPdfService;

    private BudgetService budgetService;

    @BeforeEach
    void setUp() {
        budgetService = new BudgetService(
                budgetRepository,
                clientRepository,
                serviceOrderRepository,
                userRepository,
                new BudgetMapper(),
                budgetPdfService
        );
        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(new User(OWNER_ID, "Ana", EMAIL, "encoded", LocalDateTime.now())));
    }

    @Test
    void createsPendingBudgetWithNextNumberAndOwner() {
        UUID clientId = UUID.randomUUID();
        when(clientRepository.findByIdAndOwnerId(clientId, OWNER_ID))
                .thenReturn(Optional.of(client(clientId)));
        when(budgetRepository.nextBudgetNumber()).thenReturn(41L);
        when(budgetRepository.save(any(Budget.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BudgetResponseDTO response = budgetService.create(
                EMAIL,
                request(clientId, null, "Revisão preventiva", "250.00")
        );

        ArgumentCaptor<Budget> captor = ArgumentCaptor.forClass(Budget.class);
        verify(budgetRepository).save(captor.capture());
        assertEquals(OWNER_ID, captor.getValue().getOwnerId());
        assertEquals(41L, response.budgetNumber());
        assertEquals(BudgetStatus.PENDING, response.status());
        assertEquals("Ana Cliente", response.clientName());
        assertEquals(captor.getValue().getCreatedAt().plusDays(30), response.validUntil());
    }

    @Test
    void rejectsCreationForClientFromAnotherAccount() {
        UUID clientId = UUID.randomUUID();
        when(clientRepository.findByIdAndOwnerId(clientId, OWNER_ID)).thenReturn(Optional.empty());

        assertThrows(ClientNotFoundException.class, () -> budgetService.create(
                EMAIL,
                request(clientId, null, "Revisão", "250.00")
        ));
    }

    @Test
    void rejectsServiceOrderThatBelongsToDifferentClient() {
        UUID clientId = UUID.randomUUID();
        UUID serviceOrderId = UUID.randomUUID();
        when(clientRepository.findByIdAndOwnerId(clientId, OWNER_ID))
                .thenReturn(Optional.of(client(clientId)));
        when(serviceOrderRepository.findByIdAndOwnerId(serviceOrderId, OWNER_ID))
                .thenReturn(Optional.of(serviceOrder(serviceOrderId, UUID.randomUUID())));

        assertThrows(ServiceOrderNotFoundException.class, () -> budgetService.create(
                EMAIL,
                request(clientId, serviceOrderId, "Revisão", "250.00")
        ));
    }

    @Test
    void listsBudgetsWithClientNamesForCurrentOwner() {
        UUID clientId = UUID.randomUUID();
        Budget budget = budget(UUID.randomUUID(), clientId, null, BudgetStatus.PENDING);
        when(budgetRepository.findAllByOwnerId(OWNER_ID)).thenReturn(List.of(budget));
        when(clientRepository.findAllByOwnerId(OWNER_ID)).thenReturn(List.of(client(clientId)));

        List<BudgetResponseDTO> response = budgetService.findAll(EMAIL);

        assertEquals(1, response.size());
        assertEquals("Ana Cliente", response.getFirst().clientName());
        verify(budgetRepository).findAllByOwnerId(OWNER_ID);
    }

    @Test
    void updatesBudgetDetailsWithoutChangingStatus() {
        UUID budgetId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        Budget existing = budget(budgetId, clientId, null, BudgetStatus.APPROVED);
        when(clientRepository.findByIdAndOwnerId(clientId, OWNER_ID))
                .thenReturn(Optional.of(client(clientId)));
        when(budgetRepository.findByIdAndOwnerId(budgetId, OWNER_ID)).thenReturn(Optional.of(existing));
        when(budgetRepository.save(any(Budget.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BudgetResponseDTO response = budgetService.update(
                EMAIL,
                budgetId,
                request(clientId, null, "Descrição revisada", "300.00")
        );

        assertEquals("Descrição revisada", response.description());
        assertEquals(new BigDecimal("300.00"), response.value());
        assertEquals(BudgetStatus.APPROVED, response.status());
    }

    @Test
    void changesBudgetStatus() {
        UUID budgetId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        when(budgetRepository.findByIdAndOwnerId(budgetId, OWNER_ID))
                .thenReturn(Optional.of(budget(budgetId, clientId, null, BudgetStatus.PENDING)));
        when(clientRepository.findByIdAndOwnerId(clientId, OWNER_ID))
                .thenReturn(Optional.of(client(clientId)));
        when(budgetRepository.save(any(Budget.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BudgetResponseDTO response = budgetService.updateStatus(
                EMAIL,
                budgetId,
                new BudgetStatusRequestDTO(BudgetStatus.REJECTED)
        );

        assertEquals(BudgetStatus.REJECTED, response.status());
    }

    @Test
    void doesNotExposeBudgetFromAnotherAccount() {
        UUID budgetId = UUID.randomUUID();
        when(budgetRepository.findByIdAndOwnerId(budgetId, OWNER_ID)).thenReturn(Optional.empty());

        assertThrows(BudgetNotFoundException.class, () -> budgetService.findById(EMAIL, budgetId));
    }

    @Test
    void deletesBudgetOwnedByCurrentUser() {
        UUID budgetId = UUID.randomUUID();
        Budget budget = budget(budgetId, UUID.randomUUID(), null, BudgetStatus.PENDING);
        when(budgetRepository.findByIdAndOwnerId(budgetId, OWNER_ID)).thenReturn(Optional.of(budget));

        budgetService.delete(EMAIL, budgetId);

        verify(budgetRepository).delete(budget);
    }

    @Test
    void generatesPdfForBudgetAndItsClient() {
        UUID budgetId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        Budget budget = budget(budgetId, clientId, null, BudgetStatus.PENDING);
        Client client = client(clientId);
        byte[] pdf = "%PDF-test".getBytes();
        when(budgetRepository.findByIdAndOwnerId(budgetId, OWNER_ID)).thenReturn(Optional.of(budget));
        when(clientRepository.findByIdAndOwnerId(clientId, OWNER_ID)).thenReturn(Optional.of(client));
        when(budgetPdfService.generate(budget, client)).thenReturn(pdf);

        assertArrayEquals(pdf, budgetService.generatePdf(EMAIL, budgetId));
        verify(budgetPdfService).generate(budget, client);
    }

    private BudgetRequestDTO request(UUID clientId, UUID serviceOrderId, String description, String value) {
        return new BudgetRequestDTO(clientId, serviceOrderId, description, new BigDecimal(value));
    }

    private Client client(UUID id) {
        return new Client(id, OWNER_ID, "Ana Cliente", "11999990000", "cliente@example.com", "123", LocalDateTime.now());
    }

    private ServiceOrder serviceOrder(UUID id, UUID clientId) {
        return new ServiceOrder(id, clientId, "Reparo", ServiceOrderStatus.OPEN, BigDecimal.TEN, LocalDateTime.now());
    }

    private Budget budget(UUID id, UUID clientId, UUID serviceOrderId, BudgetStatus status) {
        return new Budget(
                id,
                40L,
                OWNER_ID,
                clientId,
                serviceOrderId,
                "Revisão preventiva",
                new BigDecimal("250.00"),
                status,
                LocalDateTime.now()
        );
    }
}
