CREATE TABLE IF NOT EXISTS necrify_user
(
    uuid BINARY(16) PRIMARY KEY,
    name VARCHAR(16)
);
ALTER TABLE necrify_punishment
    ADD COLUMN uuid_new BINARY(16) FIRST;
UPDATE necrify_punishment
SET uuid_new = UUID_TO_BIN(uuid);
ALTER TABLE necrify_punishment
    DROP COLUMN uuid;
ALTER TABLE necrify_punishment
    CHANGE COLUMN uuid_new uuid BINARY(16);

ALTER TABLE necrify_punishment
    ADD COLUMN punishment_id_new BINARY(16) FIRST;
UPDATE necrify_punishment
SET punishment_id_new = UUID_TO_BIN(punishment_id);
ALTER TABLE necrify_punishment
    DROP COLUMN punishment_id;
ALTER TABLE necrify_punishment
    CHANGE COLUMN punishment_id_new punishment_id BINARY(16);

ALTER TABLE necrify_whitelist
    ADD COLUMN uuid_new BINARY(16);
UPDATE necrify_whitelist
SET uuid_new = UUID_TO_BIN(uuid);
ALTER TABLE necrify_whitelist
    DROP COLUMN uuid;
ALTER TABLE necrify_whitelist
    CHANGE COLUMN uuid_new uuid BINARY(16);
ALTER TABLE necrify_punishment
    ADD COLUMN type_new INT AFTER type;

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
    DROP COLUMN type;
ALTER TABLE necrify_punishment
    CHANGE COLUMN type_new type INT;

INSERT IGNORE INTO necrify_user (uuid, name)
SELECT DISTINCT uuid, name
FROM necrify_punishment;
INSERT IGNORE INTO necrify_user (uuid, name)
SELECT DISTINCT uuid, NULL
FROM necrify_whitelist;

DROP PROCEDURE IF EXISTS drop_constraint_if_exists;

DELIMITER //
CREATE PROCEDURE drop_constraint_if_exists(
    IN table_name VARCHAR(64),
    IN constraint_name VARCHAR(64)
)
BEGIN
    DECLARE CONTINUE HANDLER FOR SQLEXCEPTION BEGIN END;
    SET @drop_sql = CONCAT('ALTER TABLE ', table_name, ' DROP CONSTRAINT ', constraint_name);
    PREPARE stmt FROM @drop_sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
END //
DELIMITER ;

DELIMITER ;
CALL drop_constraint_if_exists('necrify_punishment', 'fk_punishment_user');
ALTER TABLE necrify_punishment
    ADD CONSTRAINT fk_punishment_user FOREIGN KEY (uuid) REFERENCES necrify_user (uuid) ON DELETE CASCADE;
CALL drop_constraint_if_exists('necrify_whitelist', 'fk_whitelist_user');
ALTER TABLE necrify_whitelist
    ADD CONSTRAINT fk_whitelist_user FOREIGN KEY (uuid) REFERENCES necrify_user (uuid) ON DELETE CASCADE;
ALTER TABLE necrify_user
    ADD COLUMN whitelisted BOOLEAN DEFAULT FALSE;
ALTER TABLE necrify_punishment
    DROP COLUMN name;
UPDATE necrify_user
SET whitelisted = TRUE
WHERE uuid IN (SELECT uuid FROM necrify_whitelist);
DROP TABLE necrify_whitelist;
ALTER TABLE necrify_punishment
    ADD COLUMN issued_at DATETIME DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE necrify_punishment
    ADD CONSTRAINT unique_punishment_id UNIQUE (punishment_id);

ALTER TABLE necrify_punishment
    ADD COLUMN successor BINARY(16) DEFAULT NULL REFERENCES necrify_punishment (punishment_id) ON DELETE SET NULL;