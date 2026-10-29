package com.gestaofacil.infrastructure.persistence;

import com.gestaofacil.domain.ServiceOrder;
import com.gestaofacil.repository.ServiceOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ServiceOrderRepositoryAdapter implements ServiceOrderRepository {

    private final SpringDataServiceOrderRepository springDataServiceOrderRepository;
    private final SpringDataClientRepository springDataClientRepository;
    private final ServiceOrderPersistenceMapper mapper;

    @Override
    public ServiceOrder save(ServiceOrder serviceOrder) {
        ClientJpaEntity client = springDataClientRepository.getReferenceById(serviceOrder.getClientId());
        return mapper.toDomain(springDataServiceOrderRepository.save(mapper.toEntity(serviceOrder, client)));
    }

    @Override
    public Optional<ServiceOrder> findByIdAndOwnerId(UUID id, UUID ownerId) {
        return springDataServiceOrderRepository.findByIdAndClient_OwnerId(id, ownerId).map(mapper::toDomain);
    }

    @Override
    public List<ServiceOrder> findAllByOwnerId(UUID ownerId) {
        return springDataServiceOrderRepository.findAllByClient_OwnerIdOrderByCreatedAtDesc(ownerId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public void delete(ServiceOrder serviceOrder) {
        springDataServiceOrderRepository.deleteById(serviceOrder.getId());
    }
}
