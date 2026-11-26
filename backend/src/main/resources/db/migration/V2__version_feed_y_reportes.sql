-- Conecta PTY — V2: "datos actualizados el ..." y reportes de usuarios del piloto

-- Fecha de generación del GTFS (feed_info.feed_version), para mostrarla en la app
ALTER TABLE importacion_datos ADD COLUMN version_feed TEXT;

-- Reportes de errores y sugerencias desde la app.
-- Privacidad por defecto: no se guarda IP, cuenta, ubicación ni identificador del teléfono.
CREATE TABLE reporte (
    id              BIGSERIAL PRIMARY KEY,
    tipo            TEXT NOT NULL CHECK (tipo IN ('ESTACION', 'RUTA', 'SUGERENCIA', 'OTRO')),
    mensaje         TEXT NOT NULL CHECK (char_length(mensaje) BETWEEN 5 AND 1000),
    parada_id       TEXT,                         -- estación relacionada, si aplica (sin FK: el catálogo se reemplaza)
    contexto        TEXT CHECK (char_length(contexto) <= 300),  -- p. ej. "Albrook → San Miguelito, 08:00"
    version_app     TEXT CHECK (char_length(version_app) <= 20),
    estado          TEXT NOT NULL DEFAULT 'nuevo' CHECK (estado IN ('nuevo', 'revisado', 'resuelto', 'descartado')),
    creado_en       TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX reporte_estado_idx ON reporte (estado, creado_en DESC);
