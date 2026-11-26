package pa.conectapty.planificacion;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pa.conectapty.comun.PeticionInvalidaException;
import pa.conectapty.planificacion.Itinerario.Plan;

@RestController
@RequestMapping("/api/v1/plan")
public class PlanControlador {

    private final ServicioPlanificacion servicio;

    public PlanControlador(ServicioPlanificacion servicio) {
        this.servicio = servicio;
    }

    /**
     * @param salida opcional, hora local de Panamá: 2026-10-26T08:00. Sin ella se usa "ahora".
     */
    @GetMapping
    public Plan planificar(
            @RequestParam @DecimalMin("7.0") @DecimalMax("10.0") double desdeLat,
            @RequestParam @DecimalMin("-83.1") @DecimalMax("-77.1") double desdeLon,
            @RequestParam @DecimalMin("7.0") @DecimalMax("10.0") double hastaLat,
            @RequestParam @DecimalMin("-83.1") @DecimalMax("-77.1") double hastaLon,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime salida) {
        LocalDateTime ahora = LocalDateTime.now(TraductorItinerario.PANAMA);
        LocalDateTime cuando = (salida == null ? ahora : salida).truncatedTo(ChronoUnit.MINUTES);
        // Límite razonable: evita consultas absurdas y llenar la caché con fechas basura
        if (cuando.isBefore(ahora.minusDays(1)) || cuando.isAfter(ahora.plusDays(60))) {
            throw new PeticionInvalidaException("La hora de salida debe estar entre ayer y los próximos 60 días.");
        }
        return servicio.planificar(r4(desdeLat), r4(desdeLon), r4(hastaLat), r4(hastaLon), cuando);
    }

    private static double r4(double v) {
        return Math.round(v * 10_000d) / 10_000d;
    }
}
