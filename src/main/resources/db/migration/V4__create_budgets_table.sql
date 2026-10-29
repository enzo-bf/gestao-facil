CREATE SEQUENCE budget_number_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE budgets (
    id UUID PRIMARY KEY,
    budget_number BIGINT NOT NULL UNIQUE,
    owner_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    client_id UUID NOT NULL REFERENCES clients (id) ON DELETE RESTRICT,
    service_order_id UUID REFERENCES service_orders (id) ON DELETE SET NULL,
    description VARCHAR(2000) NOT NULL,
    value NUMERIC(12, 2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT chk_budgets_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    CONSTRAINT chk_budgets_value_positive CHECK (value > 0)
);

CREATE INDEX idx_budgets_owner_created_at ON budgets (owner_id, created_at DESC);
