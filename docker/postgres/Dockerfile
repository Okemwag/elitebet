FROM postgres:18.1
COPY init-postgres.sh /docker-entrypoint-initdb.d/
RUN chmod +x /docker-entrypoint-initdb.d/*
USER root