CREATE TABLE IF NOT EXISTS punishment.necrify_user
(
    uuid UUID PRIMARY KEY,
    name VARCHAR(16)
);
ALTER TABLE punishment.necrify_punishment
    ADD COLUMN uuid_new UUID FIRST;
UPDATE punishment.necrify_punishment
SET uuid_new = CAST(uuid AS UUID);
ALTER TABLE punishment.necrify_punishment
    DROP COLUMN uuid;
ALTER TABLE punishment.necrify_punishment
    CHANGE COLUMN uuid_new uuid UUID;

ALTER TABLE punishment.necrify_punishment
    ADD COLUMN punishment_id_new UUID FIRST;
UPDATE punishment.necrify_punishment
SET punishment_id_new = CAST(punishment_id AS UUID);
ALTER TABLE punishment.necrify_punishment
    DROP COLUMN punishment_id;
ALTER TABLE punishment.necrify_punishment
    CHANGE COLUMN punishment_id_new punishment_id UUID;

ALTER TABLE punishment.necrify_whitelist
    ADD COLUMN IF NOT EXISTS uuid_new UUID;
UPDATE punishment.necrify_whitelist
SET uuid_new = CAST(uuid AS UUID);
ALTER TABLE punishment.necrify_whitelist
    DROP COLUMN uuid;
ALTER TABLE punishment.necrify_whitelist
    CHANGE COLUMN uuid_new uuid UUID;
ALTER TABLE punishment.necrify_punishment
    ADD COLUMN IF NOT EXISTS type_new INT AFTER type;

UPDATE punishment.necrify_punishment
SET type_new = CASE
                   WHEN type = 'BAN' THEN 1
                   WHEN type = 'PERMANENT_BAN' THEN 2
                   WHEN type = 'MUTE' THEN 3
                   WHEN type = 'PERMANENT_MUTE' THEN 4
                   WHEN type = 'KICK' THEN 5
                   ELSE 0
    END;
ALTER TABLE punishment.necrify_punishment
    DROP COLUMN IF EXISTS type;
ALTER TABLE punishment.necrify_punishment
    CHANGE COLUMN type_new type INT;

INSERT IGNORE INTO punishment.necrify_user (uuid, name)
SELECT DISTINCT uuid, name
FROM punishment.necrify_punishment;
INSERT IGNORE INTO punishment.necrify_user (uuid, name)
SELECT DISTINCT uuid, NULL
FROM punishment.necrify_whitelist;

ALTER TABLE punishment.necrify_punishment
    DROP CONSTRAINT IF EXISTS fk_punishment_user;
ALTER TABLE punishment.necrify_punishment
    ADD CONSTRAINT fk_punishment_user FOREIGN KEY (uuid) REFERENCES punishment.necrify_user (uuid) ON DELETE CASCADE;
ALTER TABLE punishment.necrify_whitelist
    DROP CONSTRAINT IF EXISTS fk_whitelist_user;
ALTER TABLE punishment.necrify_whitelist
    ADD CONSTRAINT fk_whitelist_user FOREIGN KEY (uuid) REFERENCES punishment.necrify_user (uuid) ON DELETE CASCADE;
ALTER TABLE punishment.necrify_user
    ADD COLUMN IF NOT EXISTS whitelisted BOOLEAN DEFAULT FALSE;
ALTER TABLE punishment.necrify_punishment
    DROP COLUMN IF EXISTS name;
UPDATE punishment.necrify_user
SET whitelisted = TRUE
WHERE uuid IN (SELECT uuid FROM punishment.necrify_whitelist);
DROP TABLE punishment.necrify_whitelist;
ALTER TABLE punishment.necrify_punishment
    ADD COLUMN IF NOT EXISTS issued_at DATETIME DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE punishment.necrify_punishment
    ADD CONSTRAINT unique_punishment_id UNIQUE (punishment_id);

ALTER TABLE punishment.necrify_punishment
    ADD COLUMN IF NOT EXISTS successor UUID DEFAULT NULL REFERENCES punishment.necrify_punishment (punishment_id) ON DELETE SET NULL;