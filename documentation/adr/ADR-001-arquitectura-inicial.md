# ADR-001: Arquitectura inicial del piloto

- **Estado:** aceptada
- **Fecha:** 2026-10-21

## Contexto

El documento *Resumen Integral* propone Android + Spring Boot + PostGIS + OpenTripPlanner + panel React, con 6 meses de plan. El riesgo principal es la **disponibilidad de datos**: no se encontró un GTFS oficial publicado para Metro de Panamá ni para MiBus.

## Decisiones

1. **Piloto = Metro (L1 + L2), transbordo en San Miguelito.** Pocas estaciones, red estable y datos disponibles en OpenStreetMap. MiBus queda para V2.
2. **GTFS propio generado desde OSM.** Las estaciones y su orden salen de las relaciones `route=subway` de OSM. Horarios, frecuencias y tiempos entre estaciones son **estimados** y quedan declarados como tales en `procedencia.json` y en la respuesta de la API (`calidad`).
3. **OpenTripPlanner 2.7 como motor de rutas**, consumido por GraphQL. Todo el acoplamiento vive en `ClienteOtp.java`.
4. **Monolito modular** en Spring Boot con paquetes por funcionalidad (`paradas`, `planificacion`, `salud`).
5. **JdbcClient en vez de JPA/Hibernate.** El dominio es pequeño y espacial; SQL directo con PostGIS es más simple, consume menos memoria y arranca más rápido.
6. **Android sin Hilt ni Navigation** en el Sprint 0: inyección manual y navegación por estado en un ViewModel. Menos dependencias, compilación más rápida. Se puede migrar si la app crece.
7. **Panel React pospuesto.** En el MVP los datos se cargan con `data-platform` + importador del backend (idempotente, transaccional, con historial en `importacion_datos`).
8. **Spring Security pospuesto** hasta que existan endpoints con escritura (favoritos en la nube, reportes, administración). Hoy la API es de solo lectura y sin datos personales; los favoritos viven en el teléfono.

## Consecuencias

- Se puede probar el flujo completo (buscar → resultados → detalle) sin acuerdos con operadores.
- Los tiempos mostrados son aproximados. La app lo advierte siempre.
- Cuando exista un GTFS oficial, se reemplaza `metro-pty-gtfs.zip` y no cambia nada más.
