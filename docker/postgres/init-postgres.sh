#!/bin/bash

set -e
echo '*** Creating database ***'
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
    CREATE DATABASE elitebet;
    \c elitebet
    CREATE SCHEMA elitebet_schema;
    CREATE USER elitebet_user PASSWORD 'elitebet123!';
    GRANT CONNECT ON DATABASE elitebet to elitebet_user;
    GRANT USAGE, CREATE ON SCHEMA elitebet_schema TO elitebet_user;
    GRANT SELECT,INSERT, UPDATE, DELETE, REFERENCES ON ALL TABLES IN SCHEMA elitebet_schema to elitebet_user;
    GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA elitebet_schema TO elitebet_user;
    ALTER USER elitebet_user SET search_path TO elitebet_schema, public;
EOSQL
echo '*** Database creation complete! ***'