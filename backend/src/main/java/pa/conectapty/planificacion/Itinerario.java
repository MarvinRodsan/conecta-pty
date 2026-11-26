package pa.conectapty.planificacion;

import java.util.List;

/** Respuesta pública de /api/v1/plan. Pensada para pantallas pequeñas: solo lo necesario. */
public final class Itinerario {

    private Itinerario() {}

    /**
     * @param salidaConsultada fecha y hora (Panamá) para la que se planificó, "2026-10-26T08:00"
     * @param aviso            mensaje para el usuario cuando algo importante falta (p. ej. Metro cerrado); puede ser null
     */
    public record Plan(String salidaConsultada, List<Opcion> opciones, CalidadDatos calidad, String aviso) {}

    /** @param sale / llega hora local "08:05" (aproximada) */
    public record Opcion(int duracionMin, int transbordos, int caminataM, String sale, String llega,
                         boolean usaMetro, List<Tramo> tramos) {}

    /**
     * @param modo        CAMINAR o METRO
     * @param instruccion frase lista para mostrar: "Toma la Línea 1 hacia Albrook..."
     * @param geometria   polilínea codificada (Google Encoded Polyline) para el mapa
     */
    public record Tramo(String modo, String desde, String hasta, String linea, String colorLinea,
                        String direccion, int paradas, int duracionMin, int distanciaM,
                        String sale, String llega, String instruccion, String geometria) {}

    /** Lo que la app debe advertir al usuario (sección 7.2: separar confirmado, estimado y desconocido). */
    public record CalidadDatos(String estaciones, String tiempos, String frecuencias, String tarifas, String aviso) {
        public static CalidadDatos pilotoActual() {
            return new CalidadDatos("verificado_en_osm", "estimado", "estimado", "desconocido",
                    "Tiempos aproximados: el Metro aún no publica horarios abiertos. Verifica en la estación.");
        }
    }
}
