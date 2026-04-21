CREATE DATABASE elitebet_keycloak;

CREATE USER keycloak WITH PASSWORD 'keycloak_dev_password';

GRANT ALL PRIVILEGES ON DATABASE elitebet_keycloak TO keycloak;
