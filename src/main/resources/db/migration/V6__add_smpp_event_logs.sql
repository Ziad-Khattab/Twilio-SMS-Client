-- Track SMPP session events (BIND, SUBMIT, DLR, MO, ERROR) for admin debug panel
-- Persistent across restarts

CREATE TABLE IF NOT EXISTS smpp_event_logs (
    id BIGSERIAL PRIMARY KEY,
    level VARCHAR(10) NOT NULL DEFAULT 'INFO',
    event_type VARCHAR(20) NOT NULL,
    detail TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_smpp_event_logs_created_at ON smpp_event_logs (created_at DESC);
