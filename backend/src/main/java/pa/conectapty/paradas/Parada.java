package pa.conectapty.paradas;

/**
 * Estación del Metro tal como la ve la app.
 *
 * @param distanciaM solo viene relleno en búsquedas por cercanía
 * @param accesible  "si", "no" o "desconocido": nunca asumimos accesibilidad
 */
public record Parada(String id, String nombre, double lat, double lon, Integer distanciaM, String accesible) {}
