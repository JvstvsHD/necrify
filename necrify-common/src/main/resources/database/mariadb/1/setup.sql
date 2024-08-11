CREATE TABLE IF NOT EXISTS necrify_punishment
(
    uuid          VARCHAR(36),
    name          VARCHAR(16),
    type          VARCHAR(1000),
    expiration    DATETIME,
    reason        VARCHAR(1000),
    punishment_id VARCHAR(36)
);

CREATE TABLE IF NOT EXISTS necrify_whitelist
(
    uuid VARCHAR(36)
);