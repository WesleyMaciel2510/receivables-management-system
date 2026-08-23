CREATE TABLE clients (
    id UUID PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    cpf_cnpj VARCHAR(14) NOT NULL,
    email VARCHAR(254) NOT NULL,
    phone VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT clients_status_ck CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT clients_cpf_cnpj_uq UNIQUE (cpf_cnpj),
    CONSTRAINT clients_email_uq UNIQUE (email)
);

CREATE TABLE receivables (
    id UUID PRIMARY KEY,
    client_id UUID NOT NULL,
    description VARCHAR(500) NOT NULL,
    original_amount NUMERIC(19, 2) NOT NULL,
    remaining_balance NUMERIC(19, 2) NOT NULL,
    due_date DATE NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT receivables_original_amount_ck CHECK (original_amount > 0),
    CONSTRAINT receivables_remaining_balance_ck CHECK (
        remaining_balance >= 0 AND remaining_balance <= original_amount
    ),
    CONSTRAINT receivables_status_ck CHECK (
        status IN ('PENDING', 'PARTIAL', 'PAID', 'OVERDUE', 'CANCELLED')
    ),
    CONSTRAINT receivables_client_fk FOREIGN KEY (client_id)
        REFERENCES clients (id)
        ON DELETE RESTRICT
);

CREATE INDEX receivables_client_id_idx ON receivables (client_id);
CREATE INDEX receivables_status_idx ON receivables (status);
CREATE INDEX receivables_due_date_idx ON receivables (due_date);
