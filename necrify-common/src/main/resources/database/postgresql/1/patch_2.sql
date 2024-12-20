CREATE TABLE IF NOT EXISTS necrify_schema.punishment_log (
    id INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    punishment_id UUID,
    player_id UUID REFERENCES necrify_schema.necrify_user (uuid) ON DELETE SET NULL,
    message TEXT DEFAULT NULL,
    expiration TIMESTAMP DEFAULT NULL,
    reason TEXT DEFAULT NULL,
    predecessor UUID DEFAULT NULL,
    successor UUID DEFAULT NULL,
    action VARCHAR(128) NOT NULL,
    begins_at TIMESTAMP DEFAULT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
)