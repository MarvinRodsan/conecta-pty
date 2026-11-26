#!/bin/sh
# Respaldo diario de la base de datos (formato custom de pg_dump: comprimido y restaurable por partes).
# Cron sugerido (crontab -e del usuario que maneja Docker):
#   15 3 * * *  /opt/conecta-pty/infrastructure/produccion/respaldos/respaldar.sh >> /var/log/conecta-respaldo.log 2>&1
#
# DESTINO y DIAS se pueden cambiar con variables de entorno.
# Un respaldo en el mismo servidor no protege si se pierde el servidor: copia DESTINO fuera
# (rclone a un bucket, o el respaldo automático del proveedor del VPS). Ver README.md.
set -eu

cd "$(dirname "$0")/.."
DESTINO="${DESTINO:-/var/backups/conecta}"
DIAS="${DIAS:-14}"

mkdir -p "$DESTINO"
chmod 700 "$DESTINO"
archivo="$DESTINO/conecta-$(date +%Y-%m-%d-%H%M).dump"

# Se escribe a .tmp y se renombra al final: un respaldo a medias nunca parece válido
docker compose exec -T db sh -c 'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc' > "$archivo.tmp"
mv "$archivo.tmp" "$archivo"
echo "$(date -Is) respaldo listo: $archivo ($(du -h "$archivo" | cut -f1))"

find "$DESTINO" -name 'conecta-*.dump' -mtime +"$DIAS" -delete
