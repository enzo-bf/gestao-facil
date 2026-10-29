package com.gestaofacil.repository;

import com.gestaofacil.domain.Client;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClientRepository {

    Client save(Client client);

    Optional<Client> findByIdAndOwnerId(UUID id, UUID ownerId);

    List<Client> findAllByOwnerId(UUID ownerId);

    void delete(Client client);
}
