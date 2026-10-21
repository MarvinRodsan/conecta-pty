package pa.conectapty.paradas;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/paradas")
public class ParadaControlador {

    private final ParadaRepositorio repo;

    public ParadaControlador(ParadaRepositorio repo) {
        this.repo = repo;
    }

    /** Catálogo completo. La app lo guarda en Room y lo reutiliza sin red (cache 1 h). */
    @GetMapping
    public ResponseEntity<List<Parada>> todas() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePublic())
                .body(repo.todas());
    }

    @GetMapping("/cercanas")
    public List<Parada> cercanas(
            @RequestParam @DecimalMin("7.0") @DecimalMax("10.0") double lat,
            @RequestParam @DecimalMin("-83.1") @DecimalMax("-77.1") double lon,
            @RequestParam(defaultValue = "1000") @Min(50) @Max(5000) int radio,
            @RequestParam(defaultValue = "5") @Min(1) @Max(20) int limite) {
        return repo.cercanas(lat, lon, radio, limite);
    }

    @GetMapping("/buscar")
    public List<Parada> buscar(
            @RequestParam @Size(min = 2, max = 60) String q,
            @RequestParam(defaultValue = "10") @Min(1) @Max(30) int limite) {
        return repo.buscar(q, limite);
    }
}
