#!/bin/sh
# Prueba que el último respaldo SÍ se puede restaurar, en un PostgreSQL temporal aparte
# (no toca la base de producción). Un respaldo que nunca se probó no es un respaldo.
# Cron sugerido (domingos):
#   45 3 * * 0  /opt/conecta-pty/infrastructure/produccion/respaldos/probar-restauracion.sh >> /var/log/conecta-respaldo.log 2>&1
set -eu

DESTINO="${DESTINO:-/var/backups/conecta}"
ultimo="$(ls -1t "$DESTINO"/conecta-*.dump 2>/dev/null | head -n 1)"
[ -n "$ultimo" ] || { echo "No hay respaldos en $DESTINO"; exit 1; }

nombre="conecta-prueba-restauracion"
docker rm -f "$nombre" >/dev/null 2>&1 || true
trap 'docker rm -f "$nombre" >/dev/null 2>&1 || true' EXIT

docker run -d --name "$nombre" --memory 512m -e POSTGRES_PASSWORD=prueba postgis/postgis:16-3.4 >/dev/null
# La imagen arranca PostgreSQL, instala PostGIS y lo reinicia: esperar a que termine ese primer paso
intentos=0
until docker logs "$nombre" 2>&1 | grep -q "init process complete" \
      && docker exec "$nombre" pg_isready -U postgres -h localhost >/dev/null 2>&1; do
    intentos=$((intentos + 1)); [ "$intentos" -lt 90 ] || { echo "PostgreSQL temporal no arrancó"; exit 1; }
    sleep 2
done

# template0: base vacía, sin la extensión PostGIS precargada (la trae el respaldo)
docker exec "$nombre" createdb -U postgres -T template0 conecta
docker exec -i "$nombre" pg_restore -U postgres -d conecta --no-owner --no-privileges --exit-on-error < "$ultimo"

estaciones="$(docker exec "$nombre" psql -U postgres -d conecta -tAc 'SELECT count(*) FROM parada')"
migraciones="$(docker exec "$nombre" psql -U postgres -d conecta -tAc 'SELECT count(*) FROM flyway_schema_history WHERE success')"
if [ "$estaciones" -gt 0 ] && [ "$migraciones" -gt 0 ]; then
    echo "$(date -Is) restauración OK: $(basename "$ultimo") -> $estaciones paradas, $migraciones migraciones"
else
    echo "$(date -Is) restauración FALLÓ: $(basename "$ultimo") -> $estaciones paradas, $migraciones migraciones"
    exit 1
fi
