-- Twilio SMS Client — full schema (fresh install / empty database)
-- Apply once on Neon SQL Editor or: psql ... -f database.sql
-- Do NOT run alterations.sql afterward — it is only for upgrading old databases.

CREATE TYPE user_role AS ENUM ('customer', 'administrator');
CREATE TYPE message_status AS ENUM ('pending', 'delivered', 'failed');

CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role user_role NOT NULL DEFAULT 'customer',

    -- Customer profile (NULL for administrator accounts)
    full_name VARCHAR(100),
    birthday DATE,
    msisdn VARCHAR(20) UNIQUE,
    job VARCHAR(100),
    email VARCHAR(255) UNIQUE,
    address TEXT,
    twilio_account_sid VARCHAR(34),
    twilio_auth_token VARCHAR(255),
    twilio_sender_id VARCHAR(34),

    msisdn_validated BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE sms_history (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    from_phone VARCHAR(20) NOT NULL,
    to_phone VARCHAR(20) NOT NULL,
    message TEXT NOT NULL,
    status message_status NOT NULL DEFAULT 'pending',
    sent_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_msisdn ON users(msisdn);
CREATE INDEX idx_users_email ON users(email);

CREATE INDEX idx_sms_history_user_id ON sms_history(user_id);
CREATE INDEX idx_sms_history_sent_at ON sms_history(sent_at);
CREATE INDEX idx_sms_history_from_phone ON sms_history(from_phone);
CREATE INDEX idx_sms_history_to_phone ON sms_history(to_phone);

CREATE OR REPLACE FUNCTION set_users_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE PROCEDURE set_users_updated_at();
