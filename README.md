# Conecta PTY

*Tu ciudad. Tus rutas. Tu destino.*

Planificador de viajes en transporte público para Panamá. **Piloto:** Metro de Panamá (Líneas 1 y 2), con San Miguelito como estación de transbordo.

> Estado: **MVP completo, listo para el piloto.** App con mapa, búsqueda de direcciones, hora de salida y reportes; backend con HTTPS, respaldos y monitoreo listos para un servidor. Los tiempos y frecuencias siguen siendo **estimados** hasta tener datos oficiales (ver `data-platform/salida/procedencia.json`). Lo que queda está en [Próximos pasos](#próximos-pasos).

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
| `infrastructure/` | Docker: PostgreSQL + PostGIS y OpenTripPlanner para tu PC |
| `infrastructure/produccion/` | Servidor: Caddy (HTTPS), firewall, respaldos, monitoreo |
| `backend/` | API REST `/api/v1` (Java 21, Spring Boot 4) |
| `mobile-android/` | App Android (Kotlin, Jetpack Compose, Room) |
| `admin-web/` | Panel administrativo — pospuesto (ver ADR-001) |
| `documentation/` | Decisiones (ADR), fuentes de datos, eficiencia, publicación en Google Play, revisión de reportes |

## Requisitos (Windows)

- **Docker Desktop**
- **VS Code** para datos y backend (al abrir la carpeta sugiere las extensiones) y **Android Studio** para la app
- **Python 3.11+** (solo librería estándar; `pytest` para las pruebas)
- **Android Studio** (trae su propio JDK). El backend descarga JDK 21 automáticamente si no lo tienes.
- PC con 8 GB de RAM: copia `infrastructure/wslconfig-8gb.txt` a `C:\Users\<usuario>\.wslconfig` y usa el teléfono por USB en vez del emulador. Apaga Docker con `docker compose down` cuando no lo uses.

## Puesta en marcha (primera vez)

Desde una terminal en la carpeta del proyecto:

```powershell
# 1. Datos: estaciones del Metro desde OpenStreetMap -> GTFS validado
cd data-platform
python -m conecta_data todo
cd ..

# 2. Mapa de calles de Panamá para las caminatas (una sola vez, ~50-100 MB)
cd infrastructure
powershell -ExecutionPolicy Bypass -File .\descargar_osm.ps1

# 3. Motor de rutas: armar el grafo una vez (repetir si cambia el GTFS o el mapa)
docker compose run --rm otp-construir

# 4. Base de datos + motor de rutas
docker compose up -d db otp
docker compose logs -f otp      # espera a ver "Grizzly server running", sal con Ctrl+C
cd ..

# 5. Backend
cd backend
.\gradlew.bat bootRun
```

Prueba en el navegador:

- http://localhost:8080/api/v1/salud — estado y versión de datos
- http://localhost:8080/api/v1/paradas — estaciones cargadas
- http://localhost:8080/docs — documentación OpenAPI interactiva
- http://localhost:8081 — interfaz de depuración de OpenTripPlanner

6. **App:** abre `mobile-android/` en Android Studio y espera el *Gradle Sync*. Conecta tu teléfono por USB (con *Depuración USB* activada) o abre un emulador, y en la terminal de Android Studio ejecuta:

   ```
   adb reverse tcp:8080 tcp:8080
   ```

   Así la app llega al backend de tu PC como `localhost:8080`. Repite el comando cada vez que reconectes el teléfono. Luego dale ▶ Run.

## Pruebas

```powershell
cd data-platform; python -m pytest -q
cd backend; .\gradlew.bat test
cd mobile-android; .\gradlew.bat testDebugUnitTest
```

GitHub Actions corre las tres en cada *push* (`.github/workflows/ci.yml`). Además, `seguridad.yml` revisa con Trivy la imagen del backend y busca secretos o configuraciones inseguras, y Dependabot (`.github/dependabot.yml`) propone actualizaciones de librerías cada semana.

## VS Code

Abre la **raíz del proyecto** en VS Code (la app Android se abre aparte, en Android Studio).

- **Terminal › Run Task…**: generar datos, encender o apagar Docker, arrancar el backend, conectar el teléfono (`adb reverse`) y correr las pruebas. `Ctrl+Shift+P › Tasks: Run Test Task` corre todas.
- **Run and Debug** (`Ctrl+Shift+D`): *Backend (Spring Boot)* con puntos de interrupción, y la generación de datos en Python.
- **Testing** (matraz): las pruebas de `data-platform` y del backend.

## Producción

Guía paso a paso en [`infrastructure/produccion/README.md`](infrastructure/produccion/README.md): VPS de 4 GB, Caddy con certificado automático, firewall con solo el puerto 443, usuario de base de datos con permisos mínimos, respaldo diario con prueba de restauración semanal y aviso por UptimeRobot si se cae el servidor o el motor de rutas. Para la tienda: [`documentation/publicacion.md`](documentation/publicacion.md).

## Próximos pasos

Lo que no se puede resolver con código:

1. **Datos oficiales**: pedir al Metro de Panamá horarios y frecuencias reales para reemplazar los estimados.
2. **Servidor y dominio** (US$10–25/mes + dominio): seguir la guía de producción.
3. **Google Play** (US$25): llave de firma, ficha, política de privacidad y prueba cerrada con el grupo del piloto.
4. **Piloto** con usuarios reales, revisando sus reportes (`documentation/operacion/reportes.md`).
5. Después de validar el Metro: MiBus.

## Reglas del proyecto

1. **No inventar datos.** Cada dato dice de dónde viene y si está confirmado, estimado o es desconocido.
2. **MVP pequeño y verificable** antes de ampliar cobertura.
3. **Eficiencia desde el inicio**: ver `documentation/eficiencia.md`.

Datos de estaciones © colaboradores de OpenStreetMap, licencia ODbL.
