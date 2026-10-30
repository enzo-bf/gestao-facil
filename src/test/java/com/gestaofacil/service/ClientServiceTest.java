package com.gestaofacil.service;

import com.gestaofacil.domain.Client;
import com.gestaofacil.domain.User;
import com.gestaofacil.dto.ClientRequestDTO;
import com.gestaofacil.dto.ClientResponseDTO;
import com.gestaofacil.exception.ClientNotFoundException;
import com.gestaofacil.mapper.ClientMapper;
import com.gestaofacil.repository.ClientRepository;
import com.gestaofacil.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    private static final String EMAIL = "ana@example.com";
    private static final UUID OWNER_ID = UUID.randomUUID();

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private UserRepository userRepository;

    private ClientService clientService;

    @BeforeEach
    void setUp() {
        clientService = new ClientService(clientRepository, userRepository, new ClientMapper());
        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(new User(OWNER_ID, "Ana", EMAIL, "encoded", LocalDateTime.now())));
    }

    @Test
    void createsClientForAuthenticatedOwnerAndNormalizesOptionalValues() {
        ClientRequestDTO request = new ClientRequestDTO(
                "  Cliente Exemplo  ",
                "  11999990000  ",
                " CLIENTE@EXAMPLE.COM ",
                "  123.456  "
        );
        when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ClientResponseDTO response = clientService.create(EMAIL, request);

        ArgumentCaptor<Client> captor = ArgumentCaptor.forClass(Client.class);
        verify(clientRepository).save(captor.capture());
        assertEquals(OWNER_ID, captor.getValue().getOwnerId());
        assertEquals("Cliente Exemplo", response.name());
        assertEquals("11999990000", response.phone());
        assertEquals("cliente@example.com", response.email());
        assertEquals("123456", response.document());
    }

    @Test
    void updatesOnlyClientOwnedByAuthenticatedUser() {
        UUID clientId = UUID.randomUUID();
        Client existing = client(clientId, OWNER_ID, "Nome antigo");
        when(clientRepository.findByIdAndOwnerId(clientId, OWNER_ID)).thenReturn(Optional.of(existing));
        when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ClientResponseDTO response = clientService.update(
                EMAIL,
                clientId,
                new ClientRequestDTO("Novo nome", "11999990000", null, null)
        );

        assertEquals("Novo nome", response.name());
        assertEquals(clientId, response.id());
        assertNull(response.email());
        verify(clientRepository).findByIdAndOwnerId(clientId, OWNER_ID);
    }

    @Test
    void doesNotReturnClientOwnedByAnotherUser() {
        UUID clientId = UUID.randomUUID();
        when(clientRepository.findByIdAndOwnerId(clientId, OWNER_ID)).thenReturn(Optional.empty());

        assertThrows(ClientNotFoundException.class, () -> clientService.findById(EMAIL, clientId));
        verify(clientRepository).findByIdAndOwnerId(clientId, OWNER_ID);
    }

    @Test
    void listsOnlyClientsFromAuthenticatedOwner() {
        Client client = client(UUID.randomUUID(), OWNER_ID, "Cliente");
        when(clientRepository.findAllByOwnerId(OWNER_ID)).thenReturn(List.of(client));

        List<ClientResponseDTO> response = clientService.findAll(EMAIL);

        assertEquals(List.of("Cliente"), response.stream().map(ClientResponseDTO::name).toList());
        verify(clientRepository).findAllByOwnerId(OWNER_ID);
    }

    @Test
    void deletesOnlyClientOwnedByAuthenticatedUser() {
        UUID clientId = UUID.randomUUID();
        Client client = client(clientId, OWNER_ID, "Cliente");
        when(clientRepository.findByIdAndOwnerId(clientId, OWNER_ID)).thenReturn(Optional.of(client));

        clientService.delete(EMAIL, clientId);

        verify(clientRepository).delete(client);
    }

    private Client client(UUID id, UUID ownerId, String name) {
        return new Client(
                id,
                ownerId,
                name,
                "11999990000",
                null,
                null,
                LocalDateTime.now()
        );
    }
}
