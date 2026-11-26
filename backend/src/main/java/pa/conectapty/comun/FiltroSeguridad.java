package pa.conectapty.comun;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Primera línea de defensa de la API, antes de cualquier controlador:
 * 1. Límite de peticiones por IP (429 Too Many Requests), más estricto en /plan, /lugares y /reportes.
 * 2. Cabeceras de seguridad en todas las respuestas.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class FiltroSeguridad extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(FiltroSeguridad.class);

    private final LimitadorTasa general;
    private final LimitadorTasa plan;
    private final LimitadorTasa lugares;
    private final LimitadorTasa reportes;

    public FiltroSeguridad(ConectaProperties props) {
        this.general = new LimitadorTasa(props.limites().general());
        this.plan = new LimitadorTasa(props.limites().plan());
        this.lugares = new LimitadorTasa(props.limites().lugares());
        this.reportes = new LimitadorTasa(props.limites().reportes());
    }

    /** Límite adicional según la ruta: las operaciones caras o de escritura tienen el suyo. */
    private LimitadorTasa especifico(String ruta) {
        if (ruta.startsWith("/api/v1/plan")) return plan;
        if (ruta.startsWith("/api/v1/lugares")) return lugares;
        if (ruta.startsWith("/api/v1/reportes")) return reportes;
        return null;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain cadena)
            throws ServletException, IOException {
        String ruta = req.getRequestURI();

        res.setHeader("X-Content-Type-Options", "nosniff");
        res.setHeader("X-Frame-Options", "DENY");
        res.setHeader("Referrer-Policy", "no-referrer");
        res.setHeader("Permissions-Policy", "geolocation=(), camera=(), microphone=()");
        if (ruta.startsWith("/api/")) {
            // La API solo devuelve JSON: el navegador no debe ejecutar nada que venga de aquí
            res.setHeader("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'");
        }

        if (ruta.startsWith("/api/")) {
            // getRemoteAddr: la IP real de la conexión. Detrás de un proxy, configurar
            // server.forward-headers-strategy=native y confiar SOLO en el proxy propio.
            String ip = req.getRemoteAddr();
            long espera = general.intentar(ip);
            LimitadorTasa extra = especifico(ruta);
            if (espera == 0 && extra != null) {
                espera = extra.intentar(ip);
            }
            if (espera > 0) {
                log.info("Límite de peticiones alcanzado: ip={} ruta={}", ip, ruta);
                res.setStatus(429);
                res.setHeader("Retry-After", Long.toString(espera));
                res.setContentType("application/problem+json");
                res.setCharacterEncoding("UTF-8");
                res.getWriter().write("{\"title\":\"Demasiadas peticiones\",\"status\":429,"
                        + "\"detail\":\"Espera " + espera + " segundos e inténtalo de nuevo.\"}");
                return;
            }
        }
        cadena.doFilter(req, res);
    }
}
