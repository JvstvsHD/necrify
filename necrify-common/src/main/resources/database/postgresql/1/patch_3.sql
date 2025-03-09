CREATE TABLE IF NOT EXISTS necrify_schema.necrify_punishment_template
(
    id   INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    name VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS necrify_schema.necrify_punishment_template_stage
(
    id          INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    template_id INTEGER REFERENCES necrify_schema.necrify_punishment_template (id) ON DELETE CASCADE,
    index       INTEGER,
    duration    BIGINT,
    type        INTEGER,
    reason      TEXT NOT NULL,
    UNIQUE (template_id, index)
);

CREATE TABLE IF NOT EXISTS necrify_schema.necrify_punishment_template_user_stage
(
    id          INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    user_id     UUID REFERENCES necrify_schema.necrify_user (uuid) ON DELETE CASCADE,
    template_id INTEGER REFERENCES necrify_schema.necrify_punishment_template (id) ON DELETE CASCADE,
    stage_id    INTEGER REFERENCES necrify_schema.necrify_punishment_template_stage (id) ON DELETE CASCADE
);