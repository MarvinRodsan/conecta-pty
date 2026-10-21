package pa.conectapty.planificacion;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import pa.conectapty.planificacion.Itinerario.CalidadDatos;
import pa.conectapty.planificacion.Itinerario.Plan;

@Service
public class ServicioPlanificacion {

    private final ClienteOtp otp;

    public ServicioPlanificacion(ClienteOtp otp) {
        this.otp = otp;
    }

    /**
     * Las coordenadas llegan redondeadas a 4 decimales (~11 m) desde el controlador,
     * así búsquedas casi iguales comparten caché durante 60 s y OTP no se repite.
     */
    @Cacheable("planes")
    public Plan planificar(double desdeLat, double desdeLon, double hastaLat, double hastaLon) {
        OtpRespuesta r = otp.planificar(desdeLat, desdeLon, hastaLat, hastaLon, 3);
        return new Plan(TraductorItinerario.traducir(r), CalidadDatos.pilotoActual());
    }
}
