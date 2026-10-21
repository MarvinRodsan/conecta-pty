# Inventario de fuentes de datos

Actualiza esta tabla cada vez que verifiques algo. Estados: `por revisar` · `en revisión` · `usable` · `descartada`.

| Fuente | Qué aporta | Licencia | Formato | Estado | Última revisión | Notas |
|---|---|---|---|---|---|---|
| OpenStreetMap (Overpass) | Estaciones del Metro, orden por línea, calles para caminar | ODbL 1.0 (citar) | JSON / PBF | usable | 2026-10-21 | Verificar en campo nombres y ubicación de accesos |
| Geofabrik – extracto Panamá | Red de calles para OTP | ODbL 1.0 | PBF | usable | 2026-10-21 | Se descarga con `descargar_osm.ps1` |
| Wikipedia – Panama Metro | Horario general de operación | CC BY-SA | Web | en revisión | 2026-10-21 | L-V 05:00–23:00, sáb 05:00–22:00, dom/feriados 07:00–22:00. Confirmar con el Metro |
| Metro de Panamá (sitio oficial) | Horarios, frecuencias, avisos | Por confirmar | Web | por revisar | — | Pedir por escrito si se pueden reutilizar los datos |
| MiBus | Rutas de Metrobus | Por confirmar | — | por revisar | — | No se encontró GTFS público. La comunidad OSM mapea rutas a mano |
| Datos Abiertos de Panamá | Conjuntos de transporte | Por confirmar | Varios | por revisar | — | Buscar "metro", "transporte", "paradas" |
| INEC | Estadísticas de contexto | Por confirmar | Varios | por revisar | — | Solo para justificar el problema, no para rutear |
| Mobility Database | Feeds GTFS publicados | Varía por feed | GTFS | por revisar | 2026-10-21 | Búsqueda rápida no mostró feeds de Panamá (país) |

## Lo que la app trata como estimado hoy

| Dato | Estado | Cómo se obtuvo |
|---|---|---|
| Ubicación y orden de estaciones | verificado en OSM | Relaciones `route=subway` |
| Tiempo entre estaciones | **estimado** | distancia ÷ 35 km/h + 30 s de parada |
| Frecuencia | **estimado** | 5 min L-V, 7 min sáb, 10 min dom |
| Tarifas | **desconocido** | No se publican |
| Accesibilidad | **desconocido** | No se promete |
| Feriados | no modelado | Pendiente `calendar_dates.txt` |

Todos los supuestos se editan en `data-platform/config/supuestos_metro.json`.
