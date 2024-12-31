CREATE TABLE IF NOT EXISTS necrify_schema.punishment_log
(
    id            INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    punishment_id UUID /*REFERENCES necrify_schema.necrify_punishment (punishment_id) ON DELETE NO ACTION*/,
    actor_id      UUID         REFERENCES necrify_schema.necrify_user (uuid) ON DELETE SET NULL,
    message       TEXT                  DEFAULT NULL,
    expiration    TIMESTAMP             DEFAULT NULL,
    reason        TEXT                  DEFAULT NULL,
    predecessor   UUID                  DEFAULT NULL,
    successor     UUID                  DEFAULT NULL,
    action        VARCHAR(128) NOT NULL,
    begins_at     TIMESTAMP             DEFAULT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

UPDATE necrify_schema.necrify_user
SET name = lower(name)
WHERE name IS NOT NULL;

CREATE OR REPLACE FUNCTION necrify_punishment_determine_action(_old necrify_schema.necrify_punishment,
                                                               _new necrify_schema.necrify_punishment)
    RETURNS TEXT
    LANGUAGE PLPGSQL
AS
$$
BEGIN
    IF _old.reason IS DISTINCT FROM _new.reason THEN
        RETURN 'change_reason';
    ELSIF _old.successor IS DISTINCT FROM _new.successor THEN
        -- Changes of predecessors are handled after receiving this action
        RETURN 'change_successor';
    ELSIF _old.expiration - _old.issued_at IS DISTINCT FROM _new.expiration - _new.issued_at THEN
        RETURN 'change_duration';
    ELSIF _old.expiration != _new.expiration OR _old.issued_at != _new.issued_at THEN
        RETURN 'change_time';
    ELSE
        RETURN 'unknown';
    END IF;
END;
$$;

CREATE OR REPLACE FUNCTION necrify_punishment_trigger_execute(action_param TEXT,
                                                              perform_on necrify_schema.necrify_punishment,
                                                              _message text DEFAULT NULL)
    RETURNS INTEGER
    LANGUAGE PLPGSQL
AS
$$
DECLARE
    predecessor UUID;
    actor       UUID = NULL;
    return_id   INTEGER;
BEGIN
    IF action_param = 'information' THEN
        actor := perform_on.uuid;
    END IF;
    SELECT necrify_schema.necrify_punishment.successor
    FROM necrify_schema.necrify_punishment
    WHERE necrify_schema.necrify_punishment.successor = perform_on.uuid
    INTO predecessor;
    INSERT INTO necrify_schema.punishment_log (punishment_id, actor_id, message, expiration, reason, predecessor,
                                               successor, action, begins_at)
    -- player_id remains null since as of now, there is no player associated with the action that is logged
    -- unless the action is information, since then information should get logged
    VALUES (perform_on.punishment_id, actor, _message, perform_on.expiration,
            perform_on.reason, predecessor, perform_on.successor, action_param, perform_on.issued_at)
    RETURNING id INTO return_id;
    RETURN return_id;
END;
$$;

CREATE OR REPLACE FUNCTION necrify_punishment_trigger_insert()
    RETURNS TRIGGER
    LANGUAGE PLPGSQL
AS
$$
BEGIN
    PERFORM necrify_punishment_trigger_execute('created', NEW);
    PERFORM necrify_punishment_trigger_execute('information', NEW);
    RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION necrify_punishment_trigger_update()
    RETURNS TRIGGER
    LANGUAGE PLPGSQL
AS
$$
DECLARE
    log_action    TEXT = necrify_punishment_determine_action(OLD, NEW);
    successor_var necrify_schema.necrify_punishment;
    log_id        INTEGER;
BEGIN
    PERFORM necrify_punishment_trigger_execute(log_action, NEW);
    IF log_action = 'change_successor' THEN
        SELECT * FROM necrify_schema.necrify_punishment WHERE punishment_id = NEW.successor INTO successor_var;
        log_id := necrify_punishment_trigger_execute('change_predecessor', successor_var);
        UPDATE necrify_schema.punishment_log SET predecessor = NEW.punishment_id WHERE id = log_id;
    END IF;
    RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION necrify_punishment_trigger_delete()
    RETURNS TRIGGER
    LANGUAGE PLPGSQL
AS
$$
BEGIN
    PERFORM necrify_punishment_trigger_execute('removed', old);
    RETURN NEW;
END;
$$;

CREATE OR REPLACE TRIGGER after_punishment_insert
    AFTER INSERT
    ON necrify_schema.necrify_punishment
    FOR EACH ROW
EXECUTE PROCEDURE necrify_punishment_trigger_insert();

CREATE OR REPLACE TRIGGER after_punishment_update
    AFTER UPDATE
    ON necrify_schema.necrify_punishment
    FOR EACH ROW
EXECUTE PROCEDURE necrify_punishment_trigger_update();

CREATE OR REPLACE TRIGGER after_punishment_delete
    AFTER DELETE
    ON necrify_schema.necrify_punishment
    FOR EACH ROW
EXECUTE PROCEDURE necrify_punishment_trigger_delete();