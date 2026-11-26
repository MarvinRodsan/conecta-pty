package pa.conectapty.salud;

import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Además de "estoy vivo", dice qué versión de datos se está sirviendo, para que la app
 * muestre "Datos actualizados el …" con su fuente y licencia.
 * (El detalle técnico completo está en /actuator/health.)
 */
@RestController
public class SaludControlador {

    /**
     * @param datos null si todavía no se ha importado ningún GTFS
     */
    public record Salud(String estado, String version, Datos datos) {}

    /**
     * @param actualizadoEl fecha de generación de los datos (feed_info.feed_version, "2026-10-28")
     * @param importadoEn   cuándo los cargó este servidor
     */
    public record Datos(String archivo, int registros, String actualizadoEl, OffsetDateTime importadoEn,
                        String fuente, String licencia) {}

    private final JdbcClient jdbc;
    private final String version;

    public SaludControlador(JdbcClient jdbc, Optional<org.springframework.boot.info.BuildProperties> build) {
        this.jdbc = jdbc;
        this.version = build.map(org.springframework.boot.info.BuildProperties::getVersion).orElse("dev");
    }

    @GetMapping("/api/v1/salud")
    public Salud salud() {
        Datos datos = jdbc.sql("""
                SELECT i.archivo, i.registros, i.version_feed, i.importada_en, f.nombre AS fuente, f.licencia
                FROM importacion_datos i JOIN fuente_datos f ON f.id = i.fuente_id
                WHERE i.estado = 'publicada'
                ORDER BY i.importada_en DESC LIMIT 1
                """)
                .query((rs, n) -> {
                    OffsetDateTime importada = rs.getObject("importada_en", OffsetDateTime.class);
                    String feed = rs.getString("version_feed");
                    return new Datos(rs.getString("archivo"), rs.getInt("registros"),
                            feed != null ? feed : importada.toLocalDate().toString(),
                            importada, rs.getString("fuente"), rs.getString("licencia"));
                })
                .optional().orElse(null);
        return new Salud("ok", version, datos);
    }
}
