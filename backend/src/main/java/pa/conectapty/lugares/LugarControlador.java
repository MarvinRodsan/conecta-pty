package pa.conectapty.lugares;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Locale;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Buscar por dirección o lugar: ir desde "UTP" o "Albrook Mall", no solo de estación a estación. */
@RestController
@RequestMapping("/api/v1/lugares")
public class LugarControlador {

    private final ServicioLugares servicio;

    public LugarControlador(ServicioLugares servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<Lugar> buscar(
            @RequestParam @Size(min = 3, max = 80) String q,
            @RequestParam(defaultValue = "5") @Min(1) @Max(10) int limite) {
        // Normalizar antes de la caché: "UTP", "utp " y "Utp" son la misma búsqueda
        String texto = q.strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
        return servicio.buscar(texto, limite);
    }
}
