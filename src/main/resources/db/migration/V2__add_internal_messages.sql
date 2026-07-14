CREATE TABLE IF NOT EXISTS internal_messages (
  id BIGSERIAL PRIMARY KEY,
  sender_id INT NOT NULL REFERENCES users(id),
  recipient_id INT NOT NULL REFERENCES users(id),
  content TEXT NOT NULL,
  status VARCHAR(20) DEFAULT 'sent',
  created_at TIMESTAMP DEFAULT NOW(),
  read_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_internal_recipient ON internal_messages(recipient_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_internal_sender ON internal_messages(sender_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_internal_pair ON internal_messages(LEAST(sender_id, recipient_id), GREATEST(sender_id, recipient_id), created_at DESC);
