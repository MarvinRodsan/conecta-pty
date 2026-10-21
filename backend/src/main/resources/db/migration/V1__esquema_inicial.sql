-- Conecta PTY — esquema inicial (Sprint 0)
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS unaccent;

-- Procedencia de los datos (sección 7.2): de dónde viene cada cosa y cuándo se verificó
CREATE TABLE fuente_datos (
    id              BIGSERIAL PRIMARY KEY,
    nombre          TEXT NOT NULL UNIQUE,
    licencia        TEXT,
    url             TEXT,
    ultima_verificacion TIMESTAMPTZ
);

CREATE TABLE importacion_datos (
    id              BIGSERIAL PRIMARY KEY,
    fuente_id       BIGINT NOT NULL REFERENCES fuente_datos(id),
    archivo         TEXT NOT NULL,
    sha256          CHAR(64) NOT NULL,            -- evita reimportar el mismo archivo publicado
    registros       INT NOT NULL,
    estado          TEXT NOT NULL CHECK (estado IN ('publicada', 'revertida', 'fallida')),
    importada_en    TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX importacion_sha_idx ON importacion_datos (sha256);

-- Estaciones y andenes (GTFS stops)
CREATE TABLE parada (
    id              TEXT PRIMARY KEY,             -- stop_id del GTFS
    nombre          TEXT NOT NULL,
    ubicacion       GEOGRAPHY(Point, 4326) NOT NULL,
    tipo            SMALLINT NOT NULL DEFAULT 0,  -- 0 andén, 1 estación
    estacion_padre  TEXT REFERENCES parada(id) DEFERRABLE INITIALLY DEFERRED,
    accesible       SMALLINT NOT NULL DEFAULT 0,  -- 0 desconocido, 1 sí, 2 no
    importacion_id  BIGINT NOT NULL REFERENCES importacion_datos(id)
);

-- Índice espacial: "paradas cercanas" en milisegundos aunque haya miles
CREATE INDEX parada_ubicacion_gix ON parada USING GIST (ubicacion);
CREATE INDEX parada_tipo_idx ON parada (tipo);

INSERT INTO fuente_datos (nombre, licencia, url)
VALUES ('OpenStreetMap', 'ODbL 1.0', 'https://www.openstreetmap.org/copyright');
