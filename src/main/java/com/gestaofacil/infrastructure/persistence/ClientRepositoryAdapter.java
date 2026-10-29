package com.gestaofacil.infrastructure.persistence;

import com.gestaofacil.domain.Client;
import com.gestaofacil.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ClientRepositoryAdapter implements ClientRepository {

    private final SpringDataClientRepository springDataClientRepository;
    private final ClientPersistenceMapper mapper;

    @Override
    public Client save(Client client) {
        return mapper.toDomain(springDataClientRepository.save(mapper.toEntity(client)));
    }

    @Override
    public Optional<Client> findByIdAndOwnerId(UUID id, UUID ownerId) {
        return springDataClientRepository.findByIdAndOwnerId(id, ownerId).map(mapper::toDomain);
    }

    @Override
    public List<Client> findAllByOwnerId(UUID ownerId) {
        return springDataClientRepository.findAllByOwnerIdOrderByNameAscCreatedAtDesc(ownerId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public void delete(Client client) {
        springDataClientRepository.delete(mapper.toEntity(client));
    }
}
