#!/bin/sh
# Crea el usuario de la app con permisos mínimos. PostgreSQL lo ejecuta UNA vez,
# cuando el volumen de datos está vacío (primer arranque). Corre después de 10_postgis.sh.
#
#   conecta_admin  dueño de las tablas: Flyway (migraciones) y respaldos
#   conecta_app    el backend: SELECT/INSERT/UPDATE/DELETE, sin crear ni borrar tablas
set -eu

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" \
     -v app_pass="$DB_APP_PASSWORD" -v admin="$POSTGRES_USER" -v bd="$POSTGRES_DB" <<'SQL'
CREATE ROLE conecta_app LOGIN PASSWORD :'app_pass';

-- Nadie más que estos dos usuarios entra a la base ni crea objetos en ella
REVOKE ALL ON DATABASE :"bd" FROM PUBLIC;
GRANT CONNECT ON DATABASE :"bd" TO conecta_app;
REVOKE CREATE ON SCHEMA public FROM PUBLIC;
GRANT USAGE ON SCHEMA public TO conecta_app;

-- Las tablas las crea Flyway (como conecta_admin) después; estos permisos se aplican solos
ALTER DEFAULT PRIVILEGES FOR ROLE :"admin" IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO conecta_app;
ALTER DEFAULT PRIVILEGES FOR ROLE :"admin" IN SCHEMA public
    GRANT USAGE, SELECT ON SEQUENCES TO conecta_app;
SQL
