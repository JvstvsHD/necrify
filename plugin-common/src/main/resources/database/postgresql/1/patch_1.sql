ALTER TABLE punishment.necrify_punishment
    ALTER COLUMN uuid TYPE UUID USING uuid::uuid;
ALTER TABLE punishment.necrify_punishment
    ALTER COLUMN punishment_id TYPE uuid USING punishment_id::uuid;
ALTER TABLE punishment.necrify_whitelist
    ALTER COLUMN uuid TYPE uuid USING uuid::uuid;
ALTER TABLE punishment.necrify_punishment
    ALTER COLUMN type TYPE INTEGER USING CASE
                                             WHEN (type = 'BAN') THEN 1
                                             WHEN (type = 'PERMANENT_BAN') THEN 2
                                             WHEN (type = 'MUTE') THEN 3
                                             WHEN (type = 'PERMANENT_MUTE') THEN 4
                                             WHEN (type = 'KICK') THEN 5
                                             ELSE 0
        END;