package com.gestaofacil.mapper;

import com.gestaofacil.domain.ServiceOrder;
import com.gestaofacil.dto.ServiceOrderResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class ServiceOrderMapper {

    public ServiceOrderResponseDTO toResponse(ServiceOrder serviceOrder) {
        return new ServiceOrderResponseDTO(
                serviceOrder.getId(),
                serviceOrder.getDescription(),
                serviceOrder.getStatus(),
                serviceOrder.getValue(),
                serviceOrder.getClientId(),
                serviceOrder.getCreatedAt()
        );
    }
}
