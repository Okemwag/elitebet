ARG POSTGRES_IMAGE=postgres:18.3-alpine
FROM ${POSTGRES_IMAGE}

COPY docker/postgres/init/ /docker-entrypoint-initdb.d/
