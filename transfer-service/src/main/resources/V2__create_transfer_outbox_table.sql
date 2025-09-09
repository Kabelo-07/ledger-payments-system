CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE transfer_outbox_event (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  transfer_id UUID NOT NULL,
  payload text NOT NULL,
  status VARCHAR(50) NOT NULL CHECK (status IN ('PENDING', 'PROCESSED', 'FAILED')),
  number_of_attempts INTEGER NOT NULL DEFAULT 0,
  next_attempt_at TIMESTAMP NOT NULL DEFAULT now(),
  created_at TIMESTAMP NOT NULL DEFAULT now(),
  updated_at TIMESTAMP NOT NULL DEFAULT now(),
  CONSTRAINT fk_transfer_id FOREIGN KEY (transfer_id) REFERENCES transfer(id)
);