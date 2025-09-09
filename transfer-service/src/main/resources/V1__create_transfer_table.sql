CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE transfer (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  from_account_id UUID NOT NULL,
  to_account_id UUID NOT NULL,
  amount NUMERIC(18,2) NOT NULL,
  status VARCHAR(50) NOT NULL CHECK (status IN ('PROCESSING', 'COMPLETED', 'FAILED')),
  created_at TIMESTAMP NOT NULL DEFAULT now(),
  updated_at TIMESTAMP NOT NULL DEFAULT now()
);