CREATE TABLE service_orders (
    id UUID PRIMARY KEY,
    description VARCHAR(2000) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    value NUMERIC(12, 2) NOT NULL,
    client_id UUID NOT NULL REFERENCES clients (id) ON DELETE RESTRICT,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT chk_service_orders_status
        CHECK (status IN ('OPEN', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT chk_service_orders_value_non_negative
        CHECK (value >= 0)
);

CREATE INDEX idx_service_orders_client_created_at
    ON service_orders (client_id, created_at DESC);
