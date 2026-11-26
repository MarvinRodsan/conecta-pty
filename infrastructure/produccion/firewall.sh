#!/bin/sh
# Firewall del servidor (Ubuntu/Debian con ufw). Ejecutar una vez como root:
#   sudo sh firewall.sh
# Deja abiertos solo SSH (para administrar) y HTTPS (443). Todo lo demás, cerrado.
#
# Importante: Docker abre por su cuenta los puertos que un contenedor publica, aunque ufw
# los bloquee. Por eso en docker-compose.yml solo Caddy publica un puerto (443).
set -eu

PUERTO_SSH="${PUERTO_SSH:-22}"

ufw default deny incoming
ufw default allow outgoing
# "limit" bloquea una IP que intenta más de 6 conexiones en 30 s (ataques de contraseña)
ufw limit "$PUERTO_SSH"/tcp comment 'SSH'
ufw allow 443/tcp comment 'HTTPS'
ufw allow 443/udp comment 'HTTP/3'
ufw --force enable
ufw status verbose
