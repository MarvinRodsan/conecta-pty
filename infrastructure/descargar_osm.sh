#!/usr/bin/env bash
# Igual que descargar_osm.ps1, para Linux/macOS/Git Bash.
set -euo pipefail
cd "$(dirname "$0")"
mkdir -p otp/datos
curl -L --fail -o otp/datos/panama-latest.osm.pbf \
  https://download.geofabrik.de/central-america/panama-latest.osm.pbf
ls -lh otp/datos/panama-latest.osm.pbf
