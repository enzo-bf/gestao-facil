CREATE TABLE organizations (
    id UUID PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL
);

INSERT INTO organizations (id, name, created_at)
SELECT id, 'Negócio de ' || name, created_at
FROM users;

ALTER TABLE users
    ADD COLUMN organization_id UUID,
    ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN credential_version BIGINT NOT NULL DEFAULT 0;

UPDATE users SET organization_id = id;

ALTER TABLE users
    ALTER COLUMN organization_id SET NOT NULL,
    ADD CONSTRAINT fk_users_organization
        FOREIGN KEY (organization_id) REFERENCES organizations (id) ON DELETE RESTRICT;

CREATE INDEX idx_users_organization_name ON users (organization_id, name);

ALTER TABLE clients DROP CONSTRAINT clients_owner_id_fkey;
ALTER TABLE clients
    ADD CONSTRAINT fk_clients_organization
        FOREIGN KEY (owner_id) REFERENCES organizations (id) ON DELETE CASCADE;

ALTER TABLE budgets DROP CONSTRAINT budgets_owner_id_fkey;
ALTER TABLE budgets
    ADD CONSTRAINT fk_budgets_organization
        FOREIGN KEY (owner_id) REFERENCES organizations (id) ON DELETE CASCADE;

CREATE TABLE permissions (
    id UUID PRIMARY KEY,
    code VARCHAR(80) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(500) NOT NULL
);

INSERT INTO permissions (id, code, name, description) VALUES
    (gen_random_uuid(), 'CLIENT_VIEW', 'Ver clientes', 'Consultar clientes do workspace.'),
    (gen_random_uuid(), 'CLIENT_CREATE', 'Criar clientes', 'Cadastrar clientes no workspace.'),
    (gen_random_uuid(), 'CLIENT_UPDATE', 'Editar clientes', 'Editar clientes do workspace.'),
    (gen_random_uuid(), 'CLIENT_DELETE', 'Excluir clientes', 'Excluir clientes do workspace.'),
    (gen_random_uuid(), 'OS_VIEW', 'Ver ordens de serviço', 'Consultar ordens de serviço.'),
    (gen_random_uuid(), 'OS_CREATE', 'Criar ordens de serviço', 'Cadastrar ordens de serviço.'),
    (gen_random_uuid(), 'OS_UPDATE', 'Editar ordens de serviço', 'Editar ordens e seus status.'),
    (gen_random_uuid(), 'OS_DELETE', 'Excluir ordens de serviço', 'Excluir ordens de serviço.'),
    (gen_random_uuid(), 'BUDGET_VIEW', 'Ver orçamentos', 'Consultar orçamentos e baixar PDFs.'),
    (gen_random_uuid(), 'BUDGET_CREATE', 'Criar orçamentos', 'Cadastrar orçamentos.'),
    (gen_random_uuid(), 'BUDGET_UPDATE', 'Editar orçamentos', 'Editar orçamentos e seus status.'),
    (gen_random_uuid(), 'BUDGET_DELETE', 'Excluir orçamentos', 'Excluir orçamentos.'),
    (gen_random_uuid(), 'DASHBOARD_VIEW', 'Ver dashboard', 'Acessar indicadores do negócio.'),
    (gen_random_uuid(), 'SETTINGS_MANAGE', 'Gerenciar configurações', 'Administrar configurações do workspace.'),
    (gen_random_uuid(), 'USER_MANAGE', 'Gerenciar usuários', 'Convidar usuários e administrar permissões.');

CREATE TABLE user_permissions (
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permissions (id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, permission_id)
);

INSERT INTO user_permissions (user_id, permission_id)
SELECT users.id, permissions.id
FROM users
CROSS JOIN permissions;

CREATE TABLE account_action_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash CHAR(64) NOT NULL UNIQUE,
    purpose VARCHAR(30) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    consumed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_account_action_token_purpose
        CHECK (purpose IN ('VERIFY_EMAIL', 'RESET_PASSWORD', 'INVITE'))
);

CREATE INDEX idx_account_action_tokens_user_purpose
    ON account_action_tokens (user_id, purpose, expires_at DESC);

CREATE TABLE audit_events (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    user_id UUID REFERENCES users (id) ON DELETE SET NULL,
    action VARCHAR(80) NOT NULL,
    resource VARCHAR(80) NOT NULL,
    resource_id VARCHAR(120),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_events_org_created
    ON audit_events (organization_id, created_at DESC);
