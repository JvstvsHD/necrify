CREATE TABLE IF NOT EXISTS punishment_log
(
    id            INTEGER AUTO_INCREMENT PRIMARY KEY,
    punishment_id BINARY(16),
    actor_id      BINARY(16)   NULL,-- NULL REFERENCES necrify_user (uuid) ON DELETE SET NULL,
    message       TEXT                  DEFAULT NULL,
    expiration    DATETIME              DEFAULT NULL,
    reason        TEXT                  DEFAULT NULL,
    predecessor   BINARY(16)            DEFAULT NULL,
    successor     BINARY(16)            DEFAULT NULL,
    action        VARCHAR(128) NOT NULL,
    begins_at     DATETIME              DEFAULT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP()
);

DROP FUNCTION IF EXISTS necrify_punishment_determine_action;
DROP PROCEDURE IF EXISTS necrify_punishment_trigger_execute;
DROP PROCEDURE IF EXISTS necrify_punishment_trigger_update;
DROP PROCEDURE IF EXISTS necrify_punishment_trigger_insert;
DROP PROCEDURE IF EXISTS necrify_punishment_trigger_delete;
DROP TRIGGER IF EXISTS necrify_punishment_trigger_update;
DROP TRIGGER IF EXISTS after_punishment_insert;
DROP TRIGGER IF EXISTS before_punishment_delete;
DROP TRIGGER IF EXISTS after_punishment_delete;

DELIMITER $$
CREATE FUNCTION necrify_punishment_determine_action(old_reason VARCHAR(1000),
                                                    new_reason VARCHAR(1000),
                                                    old_successor BINARY(16),
                                                    new_successor BINARY(16),
                                                    old_expiration TIMESTAMP,
                                                    new_expiration TIMESTAMP,
                                                    old_issued_at TIMESTAMP,
                                                    new_issued_at TIMESTAMP)
    RETURNS TEXT DETERMINISTIC
    LANGUAGE SQL
BEGIN
    IF old_reason != new_reason THEN
        RETURN 'change_reason';
    ELSEIF !(old_successor <=> new_successor) THEN
        -- Changes of predecessors are handled after receiving this action
        RETURN 'change_successor';
    ELSEIF old_expiration - old_issued_at != new_expiration - new_issued_at THEN
        RETURN 'change_duration';
    ELSEIF old_expiration != new_expiration OR old_issued_at != new_issued_at THEN
        RETURN 'change_time';
    ELSE
        RETURN 'unknown';
    END IF;
END;
$$


DELIMITER $$
CREATE PROCEDURE necrify_punishment_trigger_execute(IN action_param TEXT,
                                                    IN perform_on_uuid BINARY(16),
                                                    IN _message text,
                                                    OUT log_id INT)
BEGIN
    DECLARE predecessor BINARY(16);
    DECLARE actor BINARY(16);
    DECLARE p_expiration DATETIME;
    DECLARE p_reason TEXT;
    DECLARE p_successor BINARY(16);
    DECLARE p_issued_at DATETIME;
    IF action_param = 'information' THEN
        SELECT uuid
        FROM necrify_punishment
        WHERE punishment_id = perform_on_uuid
        INTO actor;
    END IF;
    SELECT necrify_punishment.successor
    FROM necrify_punishment
    WHERE necrify_punishment.successor = perform_on_uuid
    INTO predecessor;
    SELECT expiration, reason, successor, issued_at
    INTO p_expiration, p_reason, p_successor, p_issued_at
    FROM necrify_punishment
    WHERE punishment_id = perform_on_uuid;
    INSERT INTO punishment_log (punishment_id, actor_id, message, expiration, reason, predecessor,
                                successor, action, begins_at)
    -- player_id remains null since as of now, there is no player associated with the action that is logged
-- unless the action is information, since then information should get logged
    VALUES (perform_on_uuid, actor, _message, p_expiration,
            p_reason, predecessor, p_successor, action_param, p_issued_at);
    SELECT LAST_INSERT_ID()
    INTO log_id;
END;
$$

DELIMITER $$
CREATE PROCEDURE necrify_punishment_trigger_update(
    IN old_reason VARCHAR(1000),
    IN new_reason VARCHAR(1000),
    IN old_successor BINARY(16),
    IN new_successor BINARY(16),
    IN old_expiration TIMESTAMP,
    IN new_expiration TIMESTAMP,
    IN old_issued_at TIMESTAMP,
    IN new_issued_at TIMESTAMP,
    IN new_punishment_id BINARY(16)
)
BEGIN
    DECLARE log_action TEXT;
    DECLARE successor_var_punishment_id BINARY(16);
    DECLARE log_id INT;
    -- Determine the action
    SET log_action =
            necrify_punishment_determine_action(old_reason, new_reason, old_successor, new_successor, old_expiration,
                                                new_expiration, old_issued_at, new_issued_at);

-- Execute action
    CALL necrify_punishment_trigger_execute(log_action, new_punishment_id, NULL, log_id);

-- Handle successor logic
    IF log_action = 'change_successor' THEN
        SELECT punishment_id
        INTO successor_var_punishment_id
        FROM necrify_punishment
        WHERE punishment_id = new_successor;

-- Log the action for the successor
        CALL necrify_punishment_trigger_execute('change_predecessor', successor_var_punishment_id, NULL, log_id);

-- Update the log
        UPDATE punishment_log
        SET predecessor = new_punishment_id
        WHERE id = log_id;
    END IF;
END;
$$

DELIMITER $$
CREATE PROCEDURE necrify_punishment_trigger_insert(IN new_punishment_id BINARY(16))
BEGIN
    DECLARE log_id INT;
    CALL necrify_punishment_trigger_execute('created', new_punishment_id, NULL, log_id);
    CALL necrify_punishment_trigger_execute('information', new_punishment_id, NULL, log_id);
END;
$$

DELIMITER $$
CREATE PROCEDURE necrify_punishment_trigger_delete(IN old_punishment_id BINARY(16))
BEGIN
    DECLARE log_id INT;
    CALL necrify_punishment_trigger_execute('removed', old_punishment_id, NULL, log_id);
END;
$$

DELIMITER $$
-- Trigger to invoke the procedure
CREATE TRIGGER necrify_punishment_trigger_update
    AFTER
        UPDATE
    ON necrify_punishment
    FOR EACH ROW
BEGIN
    CALL necrify_punishment_trigger_update(
            OLD.reason,
            NEW.reason,
            OLD.successor,
            NEW.successor,
            OLD.expiration,
            NEW.expiration,
            OLD.issued_at,
            NEW.issued_at,
            NEW.punishment_id
         );
END;
$$

DELIMITER $$

CREATE TRIGGER after_punishment_insert
    AFTER
        INSERT
    ON necrify_punishment
    FOR EACH ROW
    CALL necrify_punishment_trigger_insert(NEW.punishment_id);
$$

DELIMITER $$
CREATE TRIGGER before_punishment_delete
    BEFORE
        DELETE
    ON necrify_punishment
    FOR EACH ROW
-- this also removes references to former successors
    CALL necrify_punishment_trigger_delete(OLD.punishment_id);
$$

DELIMITER $$
CREATE TRIGGER after_punishment_delete
    AFTER
        DELETE
    ON necrify_punishment
    FOR EACH ROW
BEGIN
    DECLARE log_id INT;
    IF OLD.successor IS NOT NULL THEN
        CALL necrify_punishment_trigger_execute('change_predecessor', OLD.successor, NULL, log_id);
    END IF;
END;
$$