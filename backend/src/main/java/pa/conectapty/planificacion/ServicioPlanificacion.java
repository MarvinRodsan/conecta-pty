package pa.conectapty.planificacion;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import pa.conectapty.comun.ConectaProperties;
import pa.conectapty.comun.ConfiguracionCache;
import pa.conectapty.planificacion.Itinerario.CalidadDatos;
import pa.conectapty.planificacion.Itinerario.Opcion;
import pa.conectapty.planificacion.Itinerario.Plan;

@Service
public class ServicioPlanificacion {

    private final ClienteOtp otp;
    private final String avisoSinMetro;

    public ServicioPlanificacion(ClienteOtp otp, ConectaProperties props) {
        this.otp = otp;
        this.avisoSinMetro = props.avisoSinMetro();
    }

    /**
     * Las coordenadas llegan redondeadas a 4 decimales (~11 m) y la hora al minuto desde el
     * controlador, así búsquedas casi iguales comparten caché durante 60 s y OTP no se repite.
     */
    @Cacheable(ConfiguracionCache.PLANES)
    public Plan planificar(double desdeLat, double desdeLon, double hastaLat, double hastaLon, LocalDateTime salida) {
        OtpRespuesta r = otp.planificar(desdeLat, desdeLon, hastaLat, hastaLon, salida, 3);
        List<Opcion> opciones = TraductorItinerario.traducir(r);
        boolean hayMetro = opciones.stream().anyMatch(Opcion::usaMetro);
        String aviso = hayMetro ? null : avisoSinMetro;
        return new Plan(salida.toString(), opciones, CalidadDatos.pilotoActual(), aviso);
    }
}
