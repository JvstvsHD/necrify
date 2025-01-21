CREATE TABLE IF NOT EXISTS necrify_user
(
    uuid UUID PRIMARY KEY,
    name VARCHAR(16)
);
ALTER TABLE necrify_punishment
    ADD COLUMN uuid_new UUID FIRST;
UPDATE necrify_punishment
SET uuid_new = CAST(uuid AS UUID);
ALTER TABLE necrify_punishment
    DROP COLUMN uuid;
ALTER TABLE necrify_punishment
    CHANGE COLUMN uuid_new uuid UUID;

ALTER TABLE necrify_punishment
    ADD COLUMN punishment_id_new UUID FIRST;
UPDATE necrify_punishment
SET punishment_id_new = CAST(punishment_id AS UUID);
ALTER TABLE necrify_punishment
    DROP COLUMN punishment_id;
ALTER TABLE necrify_punishment
    CHANGE COLUMN punishment_id_new punishment_id UUID;

ALTER TABLE necrify_whitelist
    ADD COLUMN IF NOT EXISTS uuid_new UUID;
UPDATE necrify_whitelist
SET uuid_new = CAST(uuid AS UUID);
ALTER TABLE necrify_whitelist
    DROP COLUMN uuid;
ALTER TABLE necrify_whitelist
    CHANGE COLUMN uuid_new uuid UUID;
ALTER TABLE necrify_punishment
    ADD COLUMN IF NOT EXISTS type_new INT AFTER type;

UPDATE necrify_punishment
SET type_new = CASE
                   WHEN type = 'BAN' THEN 1
                   WHEN type = 'PERMANENT_BAN' THEN 2
                   WHEN type = 'MUTE' THEN 3
                   WHEN type = 'PERMANENT_MUTE' THEN 4
                   WHEN type = 'KICK' THEN 5
                   ELSE 0
    END;
ALTER TABLE necrify_punishment
    DROP COLUMN IF EXISTS type;
ALTER TABLE necrify_punishment
    CHANGE COLUMN type_new type INT;

INSERT IGNORE INTO necrify_user (uuid, name)
SELECT DISTINCT uuid, name
FROM necrify_punishment;
INSERT IGNORE INTO necrify_user (uuid, name)
SELECT DISTINCT uuid, NULL
FROM necrify_whitelist;

ALTER TABLE necrify_punishment
    DROP CONSTRAINT IF EXISTS fk_punishment_user;
ALTER TABLE necrify_punishment
    ADD CONSTRAINT fk_punishment_user FOREIGN KEY (uuid) REFERENCES necrify_user (uuid) ON DELETE CASCADE;
ALTER TABLE necrify_whitelist
    DROP CONSTRAINT IF EXISTS fk_whitelist_user;
ALTER TABLE necrify_whitelist
    ADD CONSTRAINT fk_whitelist_user FOREIGN KEY (uuid) REFERENCES necrify_user (uuid) ON DELETE CASCADE;
ALTER TABLE necrify_user
    ADD COLUMN IF NOT EXISTS whitelisted BOOLEAN DEFAULT FALSE;
ALTER TABLE necrify_punishment
    DROP COLUMN IF EXISTS name;
UPDATE necrify_user
SET whitelisted = TRUE
WHERE uuid IN (SELECT uuid FROM necrify_whitelist);
DROP TABLE necrify_whitelist;
ALTER TABLE necrify_punishment
    ADD COLUMN IF NOT EXISTS issued_at DATETIME DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE necrify_punishment
    ADD CONSTRAINT unique_punishment_id UNIQUE (punishment_id);

ALTER TABLE necrify_punishment
    ADD COLUMN IF NOT EXISTS successor UUID DEFAULT NULL REFERENCES necrify_punishment (punishment_id) ON DELETE SET NULL;