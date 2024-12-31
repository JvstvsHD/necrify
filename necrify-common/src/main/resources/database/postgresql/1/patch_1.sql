-- noinspection SqlResolveForFile @ column/"name"

-- noinspection SqlResolveForFile @ table/"necrify_whitelist"

CREATE TABLE IF NOT EXISTS necrify_schema.necrify_user
(
    uuid UUID PRIMARY KEY,
    name VARCHAR(16)
);

ALTER TABLE necrify_schema.necrify_punishment
    ALTER COLUMN uuid TYPE UUID USING uuid :: uuid;
ALTER TABLE necrify_schema.necrify_punishment
    ALTER COLUMN punishment_id TYPE uuid USING punishment_id:: uuid;
ALTER TABLE necrify_schema.necrify_whitelist
    ALTER COLUMN uuid TYPE uuid USING uuid :: uuid;
ALTER TABLE necrify_schema.necrify_punishment
    ALTER COLUMN type TYPE INTEGER USING CASE
    WHEN (type = 'BAN') THEN 1
    WHEN (type = 'PERMANENT_BAN') THEN 2
    WHEN (type = 'MUTE') THEN 3
    WHEN (type = 'PERMANENT_MUTE') THEN 4
    WHEN (type = 'KICK') THEN 5
    ELSE 0
    END;

INSERT INTO necrify_schema.necrify_user (uuid, name)
SELECT DISTINCT uuid, name
FROM necrify_schema.necrify_punishment
ON CONFLICT
DO NOTHING;
INSERT INTO necrify_schema.necrify_user (uuid, name)
SELECT DISTINCT uuid, NULL
FROM necrify_schema.necrify_whitelist
ON CONFLICT
DO NOTHING;
ALTER TABLE necrify_schema.necrify_punishment
    DROP CONSTRAINT IF EXISTS fk_punishment_user;
ALTER TABLE necrify_schema.necrify_punishment
    ADD CONSTRAINT fk_punishment_user FOREIGN KEY (uuid) REFERENCES necrify_schema.necrify_user (uuid) ON DELETE CASCADE;
ALTER TABLE necrify_schema.necrify_whitelist
    DROP CONSTRAINT IF EXISTS fk_whitelist_user;
ALTER TABLE necrify_schema.necrify_whitelist
    ADD CONSTRAINT fk_whitelist_user FOREIGN KEY (uuid) REFERENCES necrify_schema.necrify_user (uuid) ON DELETE CASCADE;
ALTER TABLE necrify_schema.necrify_user
    ADD COLUMN IF NOT EXISTS whitelisted BOOLEAN DEFAULT FALSE;
ALTER TABLE necrify_schema.necrify_punishment
    DROP COLUMN IF EXISTS name;
UPDATE necrify_schema.necrify_user
SET whitelisted = TRUE
WHERE uuid IN (SELECT uuid FROM necrify_schema.necrify_whitelist);
DROP TABLE necrify_schema.necrify_whitelist;
ALTER TABLE necrify_schema.necrify_punishment ADD COLUMN IF NOT EXISTS issued_at TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE necrify_schema.necrify_punishment
    ADD CONSTRAINT unique_punishment_id UNIQUE (punishment_id);

ALTER TABLE necrify_schema.necrify_punishment
    ADD COLUMN IF NOT EXISTS successor UUID DEFAULT NULL REFERENCES necrify_schema.necrify_punishment (punishment_id) ON DELETE SET NULL;