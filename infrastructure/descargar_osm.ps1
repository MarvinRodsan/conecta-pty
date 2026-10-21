# Descarga el mapa de calles de Panamá (OpenStreetMap, extracto de Geofabrik)
# para que OpenTripPlanner pueda calcular caminatas y accesos a estaciones.
# Uso (PowerShell, desde la carpeta infrastructure):  .\descargar_osm.ps1
$ErrorActionPreference = "Stop"
$destino = Join-Path $PSScriptRoot "otp\datos\panama-latest.osm.pbf"
$url = "https://download.geofabrik.de/central-america/panama-latest.osm.pbf"
New-Item -ItemType Directory -Force -Path (Split-Path $destino) | Out-Null
Write-Host "Descargando $url ..."
Invoke-WebRequest -Uri $url -OutFile $destino
$mb = [math]::Round((Get-Item $destino).Length / 1MB, 1)
Write-Host "Listo: $destino ($mb MB)"
