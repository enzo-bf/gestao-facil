package com.gestaofacil.repository;

import com.gestaofacil.domain.ServiceOrder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceOrderRepository {

    ServiceOrder save(ServiceOrder serviceOrder);

    Optional<ServiceOrder> findByIdAndOwnerId(UUID id, UUID ownerId);

    List<ServiceOrder> findAllByOwnerId(UUID ownerId);

    void delete(ServiceOrder serviceOrder);
}
