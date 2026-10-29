package com.gestaofacil.infrastructure.persistence;

import com.gestaofacil.domain.Budget;
import com.gestaofacil.repository.BudgetRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class BudgetRepositoryAdapter implements BudgetRepository {

    private final SpringDataBudgetRepository springDataBudgetRepository;
    private final SpringDataClientRepository springDataClientRepository;
    private final SpringDataServiceOrderRepository springDataServiceOrderRepository;
    private final BudgetPersistenceMapper mapper;
    private final EntityManager entityManager;

    @Override
    public long nextBudgetNumber() {
        return ((Number) entityManager.createNativeQuery("SELECT nextval('budget_number_seq')").getSingleResult())
                .longValue();
    }

    @Override
    public Budget save(Budget budget) {
        ClientJpaEntity client = springDataClientRepository.getReferenceById(budget.getClientId());
        ServiceOrderJpaEntity serviceOrder = budget.getServiceOrderId() == null
                ? null
                : springDataServiceOrderRepository.getReferenceById(budget.getServiceOrderId());
        return mapper.toDomain(springDataBudgetRepository.save(mapper.toEntity(budget, client, serviceOrder)));
    }

    @Override
    public Optional<Budget> findByIdAndOwnerId(UUID id, UUID ownerId) {
        return springDataBudgetRepository.findByIdAndOwnerId(id, ownerId).map(mapper::toDomain);
    }

    @Override
    public List<Budget> findAllByOwnerId(UUID ownerId) {
        return springDataBudgetRepository.findAllByOwnerIdOrderByCreatedAtDesc(ownerId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public void delete(Budget budget) {
        springDataBudgetRepository.deleteById(budget.getId());
    }
}
