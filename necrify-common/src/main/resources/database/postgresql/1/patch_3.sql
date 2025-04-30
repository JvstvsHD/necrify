CREATE TABLE IF NOT EXISTS necrify_schema.necrify_template
(
    id   INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    name VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS necrify_schema.necrify_template_stage
(
    id          INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    template_id INTEGER REFERENCES necrify_schema.necrify_template (id) ON DELETE CASCADE,
    index       INTEGER,
    duration    BIGINT,
    type        INTEGER,
    reason      TEXT NOT NULL,
    UNIQUE (template_id, index)
);

CREATE TABLE IF NOT EXISTS necrify_schema.necrify_template_user_stage
(
    id          INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    user_id     UUID REFERENCES necrify_schema.necrify_user (uuid) ON DELETE CASCADE,
    template_id INTEGER REFERENCES necrify_schema.necrify_template (id) ON DELETE CASCADE,
    stage_id    INTEGER REFERENCES necrify_schema.necrify_template_stage (id) ON DELETE CASCADE,
    UNIQUE(user_id, template_id)
);

CREATE OR REPLACE FUNCTION necrify_schema.update_stage_indexes()
    RETURNS TRIGGER
    LANGUAGE plpgsql
AS
$$
BEGIN
    UPDATE necrify_schema.necrify_template_stage
    SET index = index - 1
    WHERE template_id = OLD.template_id
      AND index > OLD.index;

    RETURN OLD;
END;
$$;

CREATE OR REPLACE TRIGGER trigger_update_stage_indexes
    AFTER DELETE
    ON necrify_schema.necrify_template_stage
    FOR EACH ROW
EXECUTE FUNCTION necrify_schema.update_stage_indexes();
