package pa.conectapty.comun;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param avisoSinMetro texto que se muestra cuando ninguna opción usa el Metro (p. ej. fuera de horario)
 */
@ConfigurationProperties(prefix = "conecta")
public record ConectaProperties(Otp otp, Gtfs gtfs, Limites limites, Geocodificador geocodificador,
                                String avisoSinMetro) {
    /** @param calentar hacer una búsqueda de prueba al arrancar (se apaga en pruebas) */
    public record Otp(String url, int timeoutMs, boolean calentar) {}
    public record Gtfs(String path) {}

    /**
     * Peticiones permitidas por IP y por minuto.
     * @param general  para toda la API
     * @param plan     para /api/v1/plan (la operación cara: usa el motor de rutas)
     * @param lugares  para /api/v1/lugares (consulta un servicio externo)
     * @param reportes para /api/v1/reportes (escribe en la base de datos)
     */
    public record Limites(int general, int plan, int lugares, int reportes) {}

    /** Buscador de direcciones y lugares (Photon, sobre datos de OpenStreetMap). */
    public record Geocodificador(String url, boolean habilitado, int timeoutMs) {}
}
