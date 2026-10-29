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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceOrderServiceTest {

    private static final String EMAIL = "ana@example.com";
    private static final UUID OWNER_ID = UUID.randomUUID();

    @Mock
    private ServiceOrderRepository serviceOrderRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private UserRepository userRepository;

    private ServiceOrderService serviceOrderService;

    @BeforeEach
    void setUp() {
        serviceOrderService = new ServiceOrderService(
                serviceOrderRepository,
                clientRepository,
                userRepository,
                new ServiceOrderMapper()
        );
        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(new User(OWNER_ID, "Ana", EMAIL, "encoded", LocalDateTime.now())));
    }

    @Test
    void createsOpenOrderForClientOwnedByAuthenticatedUser() {
        UUID clientId = UUID.randomUUID();
        when(clientRepository.findByIdAndOwnerId(clientId, OWNER_ID))
                .thenReturn(Optional.of(client(clientId)));
        when(serviceOrderRepository.save(any(ServiceOrder.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ServiceOrderResponseDTO response = serviceOrderService.create(
                EMAIL,
                new ServiceOrderRequestDTO("Reparo de equipamento", new BigDecimal("125.50"), clientId)
        );

        ArgumentCaptor<ServiceOrder> captor = ArgumentCaptor.forClass(ServiceOrder.class);
        verify(serviceOrderRepository).save(captor.capture());
        assertEquals(clientId, captor.getValue().getClientId());
        assertEquals(ServiceOrderStatus.OPEN, response.status());
        assertEquals(new BigDecimal("125.50"), response.value());
    }

    @Test
    void rejectsCreationForClientOwnedByAnotherUser() {
        UUID clientId = UUID.randomUUID();
        when(clientRepository.findByIdAndOwnerId(clientId, OWNER_ID)).thenReturn(Optional.empty());

        assertThrows(ClientNotFoundException.class, () -> serviceOrderService.create(
                EMAIL,
                new ServiceOrderRequestDTO("Reparo", BigDecimal.TEN, clientId)
        ));
    }

    @Test
    void listsOnlyOrdersForAuthenticatedOwner() {
        ServiceOrder order = order(UUID.randomUUID(), UUID.randomUUID(), ServiceOrderStatus.OPEN);
        when(serviceOrderRepository.findAllByOwnerId(OWNER_ID)).thenReturn(List.of(order));

        List<ServiceOrderResponseDTO> response = serviceOrderService.findAll(EMAIL);

        assertEquals(1, response.size());
        assertEquals(order.getId(), response.getFirst().id());
        verify(serviceOrderRepository).findAllByOwnerId(OWNER_ID);
    }

    @Test
    void returnsOrderForAuthenticatedOwner() {
        UUID orderId = UUID.randomUUID();
        ServiceOrder order = order(orderId, UUID.randomUUID(), ServiceOrderStatus.IN_PROGRESS);
        when(serviceOrderRepository.findByIdAndOwnerId(orderId, OWNER_ID)).thenReturn(Optional.of(order));

        ServiceOrderResponseDTO response = serviceOrderService.findById(EMAIL, orderId);

        assertEquals(orderId, response.id());
        assertEquals(ServiceOrderStatus.IN_PROGRESS, response.status());
    }

    @Test
    void doesNotExposeOrderOwnedByAnotherUser() {
        UUID orderId = UUID.randomUUID();
        when(serviceOrderRepository.findByIdAndOwnerId(orderId, OWNER_ID)).thenReturn(Optional.empty());

        assertThrows(ServiceOrderNotFoundException.class, () -> serviceOrderService.findById(EMAIL, orderId));
    }

    @Test
    void updatesDescriptionValueAndClientWithoutChangingStatus() {
        UUID orderId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        ServiceOrder existing = order(orderId, clientId, ServiceOrderStatus.IN_PROGRESS);
        when(clientRepository.findByIdAndOwnerId(clientId, OWNER_ID))
                .thenReturn(Optional.of(client(clientId)));
        when(serviceOrderRepository.findByIdAndOwnerId(orderId, OWNER_ID)).thenReturn(Optional.of(existing));
        when(serviceOrderRepository.save(any(ServiceOrder.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ServiceOrderResponseDTO response = serviceOrderService.update(
                EMAIL,
                orderId,
                new ServiceOrderRequestDTO("Descrição atualizada", new BigDecimal("300.00"), clientId)
        );

        assertEquals("Descrição atualizada", response.description());
        assertEquals(new BigDecimal("300.00"), response.value());
        assertEquals(ServiceOrderStatus.IN_PROGRESS, response.status());
    }

    @Test
    void changesOrderStatus() {
        UUID orderId = UUID.randomUUID();
        ServiceOrder existing = order(orderId, UUID.randomUUID(), ServiceOrderStatus.OPEN);
        when(serviceOrderRepository.findByIdAndOwnerId(orderId, OWNER_ID)).thenReturn(Optional.of(existing));
        when(serviceOrderRepository.save(any(ServiceOrder.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ServiceOrderResponseDTO response = serviceOrderService.updateStatus(
                EMAIL,
                orderId,
                new ServiceOrderStatusRequestDTO(ServiceOrderStatus.COMPLETED)
        );

        assertEquals(ServiceOrderStatus.COMPLETED, response.status());
        assertEquals(existing.getDescription(), response.description());
    }

    @Test
    void deletesOrderOwnedByAuthenticatedUser() {
        UUID orderId = UUID.randomUUID();
        ServiceOrder existing = order(orderId, UUID.randomUUID(), ServiceOrderStatus.CANCELLED);
        when(serviceOrderRepository.findByIdAndOwnerId(orderId, OWNER_ID)).thenReturn(Optional.of(existing));

        serviceOrderService.delete(EMAIL, orderId);

        verify(serviceOrderRepository).delete(existing);
    }

    private Client client(UUID id) {
        return new Client(id, OWNER_ID, "Cliente", "11999990000", null, null, LocalDateTime.now());
    }

    private ServiceOrder order(UUID id, UUID clientId, ServiceOrderStatus status) {
        return new ServiceOrder(
                id,
                clientId,
                "Reparo de equipamento",
                status,
                new BigDecimal("125.50"),
                LocalDateTime.now()
        );
    }
}
