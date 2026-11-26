package pa.conectapty.reportes;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pa.conectapty.comun.PeticionInvalidaException;

/**
 * Canal de comentarios del piloto: estación mal ubicada, ruta incorrecta, sugerencias.
 *
 * Es el primer endpoint que escribe (ver ADR-002): anónimo, con límite de 5 por minuto
 * y por IP, textos acotados y SIN guardar la IP ni ningún identificador del teléfono.
 * Los reportes se revisan con SQL (ver documentation/operacion/reportes.md).
 */
@RestController
@RequestMapping("/api/v1/reportes")
public class ReporteControlador {

    private static final Logger log = LoggerFactory.getLogger(ReporteControlador.class);

    public record Recibido(long id, String mensaje) {}

    private final JdbcClient jdbc;

    public ReporteControlador(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Recibido crear(@Valid @RequestBody NuevoReporte r) {
        String mensaje = limpiar(r.mensaje());
        if (mensaje.length() < 5) {
            throw new PeticionInvalidaException("Cuéntanos un poco más (mínimo 5 letras).");
        }
        long id = jdbc.sql("""
                INSERT INTO reporte (tipo, mensaje, parada_id, contexto, version_app)
                VALUES (:tipo, :mensaje, :parada, :contexto, :version) RETURNING id
                """)
                .param("tipo", r.tipo().name())
                .param("mensaje", mensaje)
                .param("parada", vacioANull(r.paradaId()))
                .param("contexto", vacioANull(limpiar(r.contexto())))
                .param("version", vacioANull(r.versionApp()))
                .query(Long.class).single();
        log.info("Reporte {} recibido (tipo {})", id, r.tipo());
        return new Recibido(id, "¡Gracias! Revisaremos tu reporte para mejorar Conecta PTY.");
    }

    /** Quita caracteres de control (salvo saltos de línea): nada raro llega a la BD ni a los logs. */
    static String limpiar(String s) {
        return s == null ? null : s.replaceAll("[\\p{Cntrl}&&[^\n]]", "").strip();
    }

    private static String vacioANull(String s) {
        return s == null || s.isBlank() ? null : s;
    }
}
