package com.gestaofacil.repository;

import java.util.UUID;

public interface OrganizationRepository {

    void lockForUpdate(UUID organizationId);
}
