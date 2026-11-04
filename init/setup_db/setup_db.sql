-- Runs automatically via /docker-entrypoint-initdb.d against the `wiki` database.
CREATE SCHEMA IF NOT EXISTS wiki;
ALTER DATABASE wiki SET search_path TO wiki;
