package pa.conectapty.planificacion;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pa.conectapty.planificacion.Itinerario.Plan;

@RestController
@RequestMapping("/api/v1/plan")
public class PlanControlador {

    private final ServicioPlanificacion servicio;

    public PlanControlador(ServicioPlanificacion servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public Plan planificar(
            @RequestParam @DecimalMin("7.0") @DecimalMax("10.0") double desdeLat,
            @RequestParam @DecimalMin("-83.1") @DecimalMax("-77.1") double desdeLon,
            @RequestParam @DecimalMin("7.0") @DecimalMax("10.0") double hastaLat,
            @RequestParam @DecimalMin("-83.1") @DecimalMax("-77.1") double hastaLon) {
        return servicio.planificar(r4(desdeLat), r4(desdeLon), r4(hastaLat), r4(hastaLon));
    }

    private static double r4(double v) {
        return Math.round(v * 10_000d) / 10_000d;
    }
}
