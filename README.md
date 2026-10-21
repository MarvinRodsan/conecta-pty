# Conecta PTY

*Tu ciudad. Tus rutas. Tu destino.*

Planificador de viajes en transporte público para Panamá. **Piloto:** Metro de Panamá (Líneas 1 y 2), con San Miguelito como estación de transbordo.

> Estado: **Sprint 0**. El esqueleto funciona de punta a punta, pero los tiempos y frecuencias son **estimados** hasta tener datos oficiales (ver `data-platform/salida/procedencia.json`).

## Cómo encaja todo

```
OpenStreetMap ──► data-platform (Python) ──► metro-pty-gtfs.zip
                                                  │
                         ┌────────────────────────┴───────────┐
                         ▼                                    ▼
              OpenTripPlanner (Docker)              backend (Spring Boot)
              calcula las rutas                     importa estaciones a PostGIS
                         ▲                                    │
                         └──────── GraphQL ◄──────────────────┤
                                                              ▼
                                                 mobile-android (Compose)
```

| Carpeta | Qué es |
|---|---|
| `data-platform/` | Descarga estaciones de OSM, genera y valida el GTFS |
| `infrastructure/` | Docker: PostgreSQL + PostGIS y OpenTripPlanner |
| `backend/` | API REST `/api/v1` (Java 21, Spring Boot 3.5) |
| `mobile-android/` | App Android (Kotlin, Jetpack Compose, Room) |
| `admin-web/` | Panel administrativo — pospuesto (ver ADR-001) |
| `documentation/` | Arquitectura, decisiones (ADR), fuentes de datos, guía de eficiencia |

## Requisitos (Windows)

- **Docker Desktop**
- **Python 3.11+** (solo librería estándar; `pytest` para las pruebas)
- **Android Studio** (trae su propio JDK). El backend descarga JDK 21 automáticamente si no lo tienes.
- ~4 GB de RAM libres para OpenTripPlanner

## Puesta en marcha (primera vez)

Desde una terminal en la carpeta del proyecto:

```powershell
# 1. Datos: estaciones del Metro desde OpenStreetMap -> GTFS validado
cd data-platform
python -m conecta_data todo
cd ..

# 2. Mapa de calles de Panamá para las caminatas (una sola vez, ~50-100 MB)
cd infrastructure
.\descargar_osm.ps1

# 3. Base de datos + motor de rutas (OTP tarda 1-3 min en construir el grafo la primera vez)
docker compose up -d db otp
docker compose logs -f otp      # espera a ver "Grizzly server running"
cd ..

# 4. Backend
cd backend
.\gradlew.bat bootRun
```

Prueba en el navegador:

- http://localhost:8080/api/v1/salud — estado y versión de datos
- http://localhost:8080/api/v1/paradas — estaciones cargadas
- http://localhost:8080/docs — documentación OpenAPI interactiva
- http://localhost:8081 — interfaz de depuración de OpenTripPlanner

5. **App:** abre `mobile-android/` en Android Studio, espera el *Gradle Sync* y ejecuta en un emulador. El emulador ve tu PC como `10.0.2.2`. En un teléfono real, cambia `API_URL` en `app/build.gradle.kts` por la IP de tu PC.

## Pruebas

```powershell
cd data-platform; python -m pytest -q
cd backend; .\gradlew.bat test
cd mobile-android; .\gradlew.bat testDebugUnitTest
```

GitHub Actions corre las tres en cada *push* (`.github/workflows/ci.yml`).

## Reglas del proyecto

1. **No inventar datos.** Cada dato dice de dónde viene y si está confirmado, estimado o es desconocido.
2. **MVP pequeño y verificable** antes de ampliar cobertura.
3. **Eficiencia desde el inicio**: ver `documentation/eficiencia.md`.

Datos de estaciones © colaboradores de OpenStreetMap, licencia ODbL.
