package pa.conectapty.planificacion;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import pa.conectapty.comun.ConectaProperties;
import pa.conectapty.comun.ServicioNoDisponibleException;

/**
 * Único punto de contacto con OpenTripPlanner (API GraphQL GTFS, OTP 2.x).
 * Si algún día se cambia de motor de rutas, solo se toca esta clase.
 */
@Component
public class ClienteOtp {

    static final String CONSULTA = """
            query Plan($desdeLat: Float!, $desdeLon: Float!, $hastaLat: Float!, $hastaLon: Float!, $n: Int!) {
              plan(from: {lat: $desdeLat, lon: $desdeLon}, to: {lat: $hastaLat, lon: $hastaLon},
                   numItineraries: $n, transportModes: [{mode: WALK}, {mode: TRANSIT}]) {
                itineraries {
                  duration
                  walkDistance
                  legs {
                    mode duration distance headsign
                    from { name }
                    to { name }
                    route { shortName longName color }
                    legGeometry { points }
                    intermediateStops { name }
                  }
                }
              }
            }
            """;

    private final RestClient http;
    private final String urlOtp;

    public ClienteOtp(RestClient.Builder builder, ConectaProperties props) {
        this.urlOtp = props.otp().url();
        Duration timeout = Duration.ofMillis(props.otp().timeoutMs());
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build());
        fabrica.setReadTimeout(timeout);
        this.http = builder.baseUrl(props.otp().url()).requestFactory(fabrica).build();
    }

    public OtpRespuesta planificar(double desdeLat, double desdeLon, double hastaLat, double hastaLon, int opciones) {
        Map<String, Object> cuerpo = Map.of(
                "query", CONSULTA,
                "variables", Map.of("desdeLat", desdeLat, "desdeLon", desdeLon,
                        "hastaLat", hastaLat, "hastaLon", hastaLon, "n", opciones));
        try {
            OtpRespuesta r = http.post().uri("/otp/gtfs/v1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(cuerpo)
                    .retrieve()
                    .body(OtpRespuesta.class);
            if (r == null) {
                throw new ServicioNoDisponibleException("El motor de rutas respondió vacío.", null);
            }
            if (r.errors() != null && !r.errors().isEmpty()) {
                throw new ServicioNoDisponibleException("Error del motor de rutas: " + r.errors().get(0).message(), null);
            }
            return r;
        } catch (RestClientException e) {
            throw new ServicioNoDisponibleException(
                    "No se pudo contactar el motor de rutas (OpenTripPlanner). ¿Está corriendo en " + urlOtp + "?", e);
        }
    }
}
