CREATE TABLE users (
                       id SERIAL PRIMARY KEY,
                       username VARCHAR(20) UNIQUE NOT NULL,
                       password VARCHAR(20) NOT NULL,
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       msisdn VARCHAR(20) UNIQUE NOT NULL
);
CREATE TYPE message_status AS ENUM ('pending', 'delivered', 'failed');
CREATE TABLE sms_history (
                             id SERIAL PRIMARY KEY,
                             user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                             to_phone VARCHAR(20) NOT NULL,        -- Dummy receiver number
                             message TEXT NOT NULL,
                             status message_status DEFAULT 'pending',  -- pending, delivered, failed
                             sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_user_id ON sms_history(user_id);
CREATE INDEX idx_sent_at ON sms_history(sent_at);
