# Revisar los reportes de la app

Lo que los usuarios envían con "Reportar un problema" queda en la tabla `reporte`. No hay panel todavía (`admin-web` está pospuesto): se revisa con SQL.

No se guarda IP, cuenta, ubicación ni identificador del teléfono; solo el tipo, el mensaje, la estación y el contexto (p. ej. `Albrook → San Miguelito, 08:00`) y la versión de la app.

## Entrar a la base

```bash
# Servidor (infrastructure/produccion)
docker compose exec db psql -U conecta_admin -d conecta
# Tu PC (infrastructure)
docker compose exec db psql -U conecta -d conecta
```

## Consultas útiles

```sql
-- Nuevos, del más reciente al más viejo
SELECT id, tipo, creado_en::date AS dia, parada_id, contexto, left(mensaje, 80) AS mensaje
FROM reporte WHERE estado = 'nuevo' ORDER BY creado_en DESC;

-- Un reporte completo
SELECT * FROM reporte WHERE id = 42;

-- Estaciones con más reportes (para priorizar qué verificar en campo)
SELECT parada_id, count(*) FROM reporte
WHERE parada_id IS NOT NULL AND estado IN ('nuevo', 'revisado')
GROUP BY parada_id ORDER BY count(*) DESC;

-- Marcar como revisado / resuelto / descartado
UPDATE reporte SET estado = 'resuelto' WHERE id IN (42, 43);
```

Estados: `nuevo` → `revisado` (ya se leyó) → `resuelto` (se corrigió el dato o la app) o `descartado` (spam, duplicado, no aplica).

## Qué hacer con cada tipo

| Tipo | Acción habitual |
|---|---|
| `ESTACION` | Nombre, entrada o accesibilidad mal: corregir en OpenStreetMap o en `data-platform/config/accesibilidad_estaciones.json`, regenerar el GTFS y publicarlo |
| `RUTA` | Tiempo o transbordo raro: probar el mismo viaje en la interfaz de OTP (`http://localhost:8081`) y revisar los supuestos en `data-platform/config/supuestos_metro.json` |
| `SUGERENCIA` | Anotar en el backlog |
| `OTRO` | Leer y decidir |

Si llega mucho spam, el límite de `/api/v1/reportes` (5 por IP y minuto) se ajusta en `backend/src/main/resources/application.yml` (`conecta.limites.reportes`).
