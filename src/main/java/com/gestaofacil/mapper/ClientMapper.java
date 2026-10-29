package com.gestaofacil.mapper;

import com.gestaofacil.domain.Client;
import com.gestaofacil.dto.ClientResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class ClientMapper {

    public ClientResponseDTO toResponse(Client client) {
        return new ClientResponseDTO(
                client.getId(),
                client.getName(),
                client.getPhone(),
                client.getEmail(),
                client.getDocument(),
                client.getCreatedAt()
        );
    }
}
