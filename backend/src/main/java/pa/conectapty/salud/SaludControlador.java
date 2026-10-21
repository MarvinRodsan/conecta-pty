package pa.conectapty.salud;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint de salud del Sprint 0. Además de "estoy vivo", dice qué versión de datos
 * se está sirviendo, para poder mostrar en la app "datos actualizados el ...".
 * (El detalle técnico completo está en /actuator/health.)
 */
@RestController
public class SaludControlador {

    private final JdbcClient jdbc;

    public SaludControlador(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/api/v1/salud")
    public Map<String, Object> salud() {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("estado", "ok");
        List<Map<String, Object>> datos = jdbc.sql("""
                SELECT i.archivo, i.registros, i.importada_en, f.nombre AS fuente, f.licencia
                FROM importacion_datos i JOIN fuente_datos f ON f.id = i.fuente_id
                WHERE i.estado = 'publicada'
                ORDER BY i.importada_en DESC LIMIT 1
                """)
                .query().listOfRows();
        r.put("datos", datos.isEmpty() ? "sin datos importados" : datos.get(0));
        return r;
    }
}
