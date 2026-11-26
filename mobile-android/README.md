# mobile-android

App Android de Conecta PTY: Kotlin, Jetpack Compose, Room (favoritos y caché), Retrofit y MapLibre. Mínimo Android 8.0 (API 26).

## Abrir y correr

1. Abre **esta carpeta** (`mobile-android/`, no la raíz del proyecto) en Android Studio y espera el *Gradle Sync*.
2. Enciende el backend en tu PC (ver el README principal).
3. Conecta el teléfono por USB con *Depuración USB* activada, o abre un emulador, y en la terminal:

   ```
   adb reverse tcp:8080 tcp:8080
   ```

   La versión *debug* usa `http://localhost:8080/`; con `adb reverse` ese localhost es el de tu PC. Repite el comando al reconectar el teléfono.
4. ▶ Run.

Desde una terminal fuera de Android Studio, usa su JDK: `$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"` (PowerShell) antes de `.\gradlew.bat`.

## Qué hace

- Elegir estaciones de origen y destino, o buscar una **dirección o lugar** (pasa por `/api/v1/lugares`), o usar **mi ubicación** (permiso de ubicación aproximada o precisa; se pide solo al tocar el botón).
- Elegir **hora de salida**: ahora o una fecha y hora concretas.
- Resultados con duración, transbordos y avisos; **detalle paso a paso con mapa** (MapLibre con estilo de OpenFreeMap, sin API key).
- **Favoritos** guardados en el teléfono.
- **"Datos actualizados el …"** con fuente y licencia, desde `/api/v1/salud`.
- **Reportar un problema**: envía tipo, mensaje y contexto a `/api/v1/reportes`, sin datos personales.
- Ícono adaptable (con versión monocromo para Android 13+), pantalla de inicio (*splash*), modo oscuro y descripciones para TalkBack.

## Ajustes privados (`local.properties`)

Este archivo no se sube a GitHub. Todo es opcional:

```properties
# Reporte de fallos con Sentry (sentry.io, plan gratis). Sin DSN, Sentry no se inicia y no se envía nada.
sentry.dsn=https://xxxx@oNNNN.ingest.sentry.io/NNNN

# Servidor de la versión de publicación (release)
api.url=https://api.tu-dominio.com/

# Llave de firma para Google Play (ver documentation/publicacion.md)
firma.archivo=C:/ruta/conecta-pty.jks
firma.alias=conecta
firma.clave=...
```

En CI se pueden pasar como variables de entorno: `SENTRY_DSN`, `CONECTA_API_URL`, `FIRMA_ARCHIVO`, `FIRMA_ALIAS`, `FIRMA_CLAVE`.

Sentry envía solo el error, la versión y el modelo del teléfono: sin capturas de pantalla, sin ubicación y sin datos personales.

## Pruebas

```powershell
.\gradlew.bat testDebugUnitTest            # unitarias, sin teléfono (búsqueda, geometría, utilidades)
.\gradlew.bat connectedDebugAndroidTest    # pantallas, con teléfono o emulador
```

## Rendimiento: Baseline Profile

El módulo `baselineprofile/` le dice a Android qué código precompilar para que la app abra más rápido. Necesita un teléfono o emulador con Android 9+:

```powershell
.\gradlew.bat :app:generateBaselineProfile                          # genera app/src/release/generated/baselineProfiles
.\gradlew.bat :baselineprofile:connectedBenchmarkReleaseAndroidTest  # mide el arranque en frío
```

Vuelve a generarlo cuando cambien mucho las pantallas. Más detalles en `documentation/eficiencia.md`.

## Versión de publicación

```powershell
.\gradlew.bat bundleRelease     # app/build/outputs/bundle/release/app-release.aab (para Google Play)
.\gradlew.bat assembleRelease   # APK para instalar a mano
```

R8 reduce y optimiza el código. Sin llave propia se firma con la de depuración: sirve para probar, no para Google Play.
