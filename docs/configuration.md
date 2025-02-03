---
search:
  boost: 2
title: Necrify plugin configuration
description: Configuration of the Necrify plugin.
---
# Necrify's configuration
More is coming Soon:tm:
``` yaml
# Determines whether the whitelist should be active or not. If set to true, only users who are whitelisted may join.
whitelist-activated: false
# Allows the registration of commands on a top-level, so /necrify ban is registered as well as plainly /ban.
# /necrify user and /necrify punishment will not be registered as /user and /punishment.
allow-top-level-commands: true
# Sets the default language. This will be used as fallback option if the language of a user is not available.
default-language: "en"

# Controls settings regarding punishments
punishment:
  # Controls default messages for punishments. Message is the value to an ID. The IDs map as follows:
  # 1 - permanent ban; 2 - temporary ban; 3 - permanent mute; 4 - temporary mute; 5 - kick
  punishmentMessages:
    1: "<red>You are banned."
    2: "<red>You were permanently banned."
    3: "<red>You were muted."
    4: "<red>You were permanently muted."
    5: "<red>You were kicked."

# Controls the usage of storage for user and punishment data.
# See also in the wiki: https://github.com/JvstvsHD/necrify/wiki/Storage
database:
  # Which host to take, e.g. localhost if the database is on the same machine or an IP address.
  host: "localhost"
  password: "password"
  username: "username"
  # Which database (scheme) to use
  database: "database"
  # The port on which the database server is running. 3306 is default for MySQL and MariaDB, 5432 for PostgreSQL.
  port: "5432"
  # The schema to use for the user and punishment data. This only applies to PostgreSQL and can be ignored for other dbs.
  postgresSchema: "punishment"
  # Which type of database to use. Currently only PostgreSQL, MariaDB and MySQL are supported.
  sql-type: "postgresql"
  # Determines how many connections should be kept open at maximum. If only one connection is open at the same time,
  # this will limit the performance of the plugin as only one database transaction can be executed at the same time.
  # If a low number of concurrent players/logins is expected, this can be set to a low number.
  # Setting this to a higher value also increases the memory usage of the plugin.
  max-pool-size: 5
  # Determines how many idle connections should be kept open at minimum. As soon as the number of connections is below this
  # number, HikariCP tries to open new connections as quickly and efficiently as possible.
  min-idle: 2
  # Determines whether the version of the database schema should be reset to the previous version. This avoids problems when
  # a dev build is used and new additions to the database schema patch files are made afterwards and before the release. This
  # will go only into effect when a dev build is used.
  enable-development-version-reset: false
```