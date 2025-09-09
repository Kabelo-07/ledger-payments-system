CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE ledger_entry (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  transfer_id UUID NOT NULL,
  amount NUMERIC(18,2) NOT NULL DEFAULT 0,
  account_id UUID NOT NULL,
  type VARCHAR(50) NOT NULL CHECK (type IN ('DEBIT', 'CREDIT')),
  created_at TIMESTAMP NOT NULL DEFAULT now(),
  updated_at TIMESTAMP NOT NULL DEFAULT now(),
  CONSTRAINT uq_transfer_id_type UNIQUE (transfer_id, type),
  CONSTRAINT fk_account_id FOREIGN KEY (account_id) REFERENCES account(id)
);