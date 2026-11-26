package pa.conectapty.planificacion;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import pa.conectapty.comun.ConectaProperties;
import pa.conectapty.comun.ServicioNoDisponibleException;

/**
 * Único punto de contacto con OpenTripPlanner (API GraphQL GTFS, OTP 2.x).
 * Si algún día se cambia de motor de rutas, solo se toca esta clase.
 */
@Component
public class ClienteOtp {

    private static final Logger log = LoggerFactory.getLogger(ClienteOtp.class);

    static final String CONSULTA = """
            query Plan($desdeLat: Float!, $desdeLon: Float!, $hastaLat: Float!, $hastaLon: Float!, $n: Int!,
                       $fecha: String!, $hora: String!) {
              plan(from: {lat: $desdeLat, lon: $desdeLon}, to: {lat: $hastaLat, lon: $hastaLon},
                   date: $fecha, time: $hora,
                   numItineraries: $n, transportModes: [{mode: WALK}, {mode: TRANSIT}]) {
                itineraries {
                  duration
                  walkDistance
                  legs {
                    mode duration distance headsign startTime endTime
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
        // HTTP/1.1 explícito: por defecto Java intenta negociar HTTP/2 ("h2c") y el servidor
        // de OTP (Grizzly) no siempre lo acepta en peticiones POST.
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder()
                        .version(HttpClient.Version.HTTP_1_1)
                        .connectTimeout(Duration.ofSeconds(3))
                        .build());
        fabrica.setReadTimeout(timeout);
        this.http = builder.baseUrl(props.otp().url()).requestFactory(fabrica).build();
    }

    /** @param salida fecha y hora local de Panamá desde la que se busca */
    public OtpRespuesta planificar(double desdeLat, double desdeLon, double hastaLat, double hastaLon,
                                   java.time.LocalDateTime salida, int opciones) {
        Map<String, Object> variables = new java.util.HashMap<>();
        variables.put("desdeLat", desdeLat);
        variables.put("desdeLon", desdeLon);
        variables.put("hastaLat", hastaLat);
        variables.put("hastaLon", hastaLon);
        variables.put("n", opciones);
        variables.put("fecha", salida.toLocalDate().toString());                 // 2026-10-26
        variables.put("hora", salida.toLocalTime().withSecond(0).withNano(0).toString()); // 08:00
        Map<String, Object> cuerpo = Map.of("query", CONSULTA, "variables", variables);
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
                log.warn("OTP respondió con error GraphQL: {}", r.errors());
                throw new ServicioNoDisponibleException("El motor de rutas no pudo calcular la ruta.", null);
            }
            return r;
        } catch (RestClientResponseException e) {
            log.warn("OTP respondió HTTP {}: {}", e.getStatusCode().value(), e.getResponseBodyAsString());
            throw new ServicioNoDisponibleException("El motor de rutas respondió con un error.", e);
        } catch (ResourceAccessException e) {
            log.warn("No se pudo conectar con OTP en {}: {}", urlOtp, e.getMessage());
            throw new ServicioNoDisponibleException(
                    "No se pudo contactar el motor de rutas. Inténtalo de nuevo en unos segundos.", e);
        } catch (RestClientException e) {
            log.warn("Respuesta inesperada de OTP", e);
            throw new ServicioNoDisponibleException("Respuesta inesperada del motor de rutas.", e);
        }
    }
}
