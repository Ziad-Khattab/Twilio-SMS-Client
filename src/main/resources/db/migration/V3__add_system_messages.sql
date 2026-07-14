CREATE TABLE IF NOT EXISTS system_messages (
  id BIGSERIAL PRIMARY KEY,
  content TEXT NOT NULL,
  created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS system_message_reads (
  user_id INT NOT NULL REFERENCES users(id),
  last_read_id BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (user_id)
);

CREATE INDEX IF NOT EXISTS idx_system_created ON system_messages(created_at DESC);
