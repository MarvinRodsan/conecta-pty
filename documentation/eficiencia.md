# Guía de eficiencia de Conecta PTY

Una app de transporte se usa con poca batería, datos móviles limitados y señal intermitente (dentro del Metro). Estas reglas ya están aplicadas en el código y hay que mantenerlas.

## App Android

| Regla | Dónde está | Por qué |
|---|---|---|
| Catálogo de estaciones en Room, refresco cada 12 h | `Repositorio.refrescarEstaciones` | Elegir origen y destino funciona sin red y sin gastar datos |
| Caché HTTP de 5 MB que respeta `Cache-Control` | `ConectaApi.crear` | El servidor marca el catálogo como válido por 1 h |
| kotlinx.serialization (sin reflexión) | `Dtos.kt` | Más rápido que Gson y compatible con R8 |
| R8 + `shrinkResources` en release | `app/build.gradle.kts` | APK más pequeño, código optimizado |
| `profileinstaller` | dependencias | Permite Baseline Profiles (Google reporta arranques en frío hasta ~30 % más rápidos; medirlo en nuestro caso) |
| Sin Hilt ni Navigation | `ConectaApp`, `PlanViewModel` | Menos dependencias, compilación y arranque más rápidos |
| `collectAsStateWithLifecycle` + `WhileSubscribed(5 s)` | `MainActivity`, `PlanViewModel` | No se lee la BD ni se recompone con la app en segundo plano |
| `LazyColumn` con `key` | selector y resultados | Compose reutiliza filas en vez de redibujar la lista |
| Filtrado con `remember(consulta, estaciones)` y clave precalculada | `SelectorEstacion`, `Estacion.claveBusqueda` | Buscar no normaliza textos en cada recomposición |
| Lista paso a paso antes que el mapa | `PantallaDetalle` | Casi no consume datos y funciona con lector de pantalla |
| Cancelar búsquedas viejas | `PlanViewModel.buscar` | Dos toques no lanzan dos peticiones |

**Siguiente nivel:** módulo `baselineprofile` con Macrobenchmark cuando las pantallas estén estables, y mapa con MapLibre (vectorial, sin API key) cargado solo en la pantalla de detalle.

## Backend

| Regla | Dónde está | Por qué |
|---|---|---|
| Hilos virtuales de Java 21 | `application.yml` | Muchas peticiones simultáneas que esperan a OTP sin bloquear hilos |
| JdbcClient en vez de JPA | `ParadaRepositorio` | Menos memoria y SQL espacial directo |
| Índice GIST + `<->` (KNN) | `V1__esquema_inicial.sql` | "Estaciones cercanas" en milisegundos |
| Caché Caffeine de 60 s por coordenadas redondeadas (~11 m) | `ServicioPlanificacion` | Búsquedas casi iguales no repiten el cálculo en OTP |
| Compresión gzip de JSON (> 1 KB) | `application.yml` | El JSON repetitivo se comprime mucho: menos datos móviles |
| Respuesta "traducida" y mínima | `TraductorItinerario` | La app recibe frases listas, no el JSON completo de OTP |
| Timeouts explícitos (3 s conexión, 8 s lectura) | `ClienteOtp` | Un OTP lento no tumba la API |
| Imagen Docker JRE + SerialGC | `backend/Dockerfile` | Menos RAM en servidores pequeños |

## Datos

- Importación **idempotente** por SHA-256: reiniciar el backend no reimporta.
- **Transaccional**: o se publica el catálogo completo o no cambia nada.
- Validación antes de publicar (`python -m conecta_data validar`): coordenadas en Panamá, referencias, tiempos crecientes, estaciones duplicadas a menos de 30 m.
