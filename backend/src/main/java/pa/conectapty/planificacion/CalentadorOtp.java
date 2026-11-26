package pa.conectapty.planificacion;

import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import pa.conectapty.comun.ConectaProperties;
import pa.conectapty.paradas.Parada;
import pa.conectapty.paradas.ParadaRepositorio;

/**
 * "Calienta" OpenTripPlanner al arrancar: la primera búsqueda de OTP es lenta (carga índices
 * y compila código en caliente). Hacemos una de prueba en segundo plano para que el primer
 * usuario real no la sufra. No bloquea el arranque y, si OTP no está listo, reintenta un rato.
 */
@Component
@Order(100) // después del importador de estaciones
public class CalentadorOtp implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CalentadorOtp.class);
    private static final int INTENTOS = 12;          // 12 x 10 s = 2 minutos como máximo
    private static final long PAUSA_MS = 10_000;

    private final ClienteOtp otp;
    private final ParadaRepositorio paradas;
    private final boolean calentar;

    public CalentadorOtp(ClienteOtp otp, ParadaRepositorio paradas, ConectaProperties props) {
        this.otp = otp;
        this.paradas = paradas;
        this.calentar = props.otp().calentar();
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!calentar) return;
        Thread.ofVirtual().name("calentador-otp").start(this::calentar);
    }

    private void calentar() {
        List<Parada> estaciones = paradas.todas();
        if (estaciones.size() < 2) {
            log.info("Sin estaciones cargadas: se omite el calentamiento de OTP.");
            return;
        }
        Parada a = estaciones.get(0);
        Parada b = estaciones.get(estaciones.size() - 1);
        // Un día laborable a media mañana: garantiza que haya trenes en la búsqueda
        LocalDateTime salida = LocalDateTime.now(TraductorItinerario.PANAMA)
                .plusDays(1).withHour(9).withMinute(0).withSecond(0).withNano(0);
        while (salida.getDayOfWeek().getValue() >= 6) salida = salida.plusDays(1);

        for (int i = 1; i <= INTENTOS; i++) {
            long t0 = System.nanoTime();
            try {
                otp.planificar(a.lat(), a.lon(), b.lat(), b.lon(), salida, 1);
                log.info("OTP listo: búsqueda de calentamiento en {} ms ({} -> {}).",
                        (System.nanoTime() - t0) / 1_000_000, a.nombre(), b.nombre());
                return;
            } catch (RuntimeException e) {
                log.info("OTP aún no responde (intento {}/{}). Reintentando en {} s.", i, INTENTOS, PAUSA_MS / 1000);
                try {
                    Thread.sleep(PAUSA_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
        log.warn("OTP no respondió tras {} intentos. Las primeras búsquedas pueden fallar.", INTENTOS);
    }
}
