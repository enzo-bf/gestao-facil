CREATE TABLE clients (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name VARCHAR(150) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    email VARCHAR(150),
    document VARCHAR(20),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL
);

CREATE INDEX idx_clients_owner_name ON clients (owner_id, name);
