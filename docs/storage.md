---
search:
  boost: 0.5
title: Storage options for Necrify
description: Information about the storage options for Necrify.
---
# Storage

Necrify uses its configured to storage store information and data about its users and their punishments as well as the 
current version of the database schema (only for internal use). Necrify supports multiple types of storages, though at 
the moment only SQL-related ones. Currently supported are PostgreSQL, MySQL and MariaDB. There are currently no direct plans 
to support SQLite or other non-SQL-derived file-based storage types since they are not compatible with the current storage structure.

## PostgreSQL
> Download the latest version [here↗](https://www.postgresql.org/download/).

PostgreSQL (also referred to as 'Postgres') is the recommended mean of storage to use for Necrify plugins. Necrify requires 
the PostgreSQL server to run with version 8.3 at least, but only version 12 and newer are officially supported. It is 
recommended to use the latest version, at least of the running version. See [here↗](https://www.postgresql.org/support/versioning/) 
for more information regarding this topic.

## MySQL
> Download the latest version [here↗](https://www.mysql.com/downloads/).

Necrify requires the MySQL server to run with version 8.0.0 at least, though later versions probably contain important bug 
fixes and may increase the overall performance. Lower versions do not support storing UUIDs directly in tables which is why the will not work.

## MariaDB
> Download the latest version [here↗](https://mariadb.org/download/).

Necrify requires the MariaDB server to be of version 10.7 at least, though later versions probably contain important bug 
fixes and may increase the overall performance. Lower versions do not support storing UUIDs directly in tables which is why the will not work.