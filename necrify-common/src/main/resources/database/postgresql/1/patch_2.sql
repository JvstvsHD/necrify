CREATE TABLE IF NOT EXISTS punishment_log (
    id SERIAL PRIMARY KEY,
    punishment_id UUID,
    player_id UUID REFERENCES necrify_users (uuid) ON DELETE SET NULL,
    message TEXT,
    expiration TIMESTAMP,
    reason TEXT,
    predecessor UUID DEFAULT NULL,
    successor UUID DEFAULT NULL,
    action VARCHAR(128),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
)