package pa.conectapty.planificacion;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/** Forma exacta de la respuesta GraphQL de OpenTripPlanner (solo los campos que pedimos). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OtpRespuesta(Data data, List<ErrorGql> errors) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Data(PlanOtp plan) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PlanOtp(List<ItinerarioOtp> itineraries) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ItinerarioOtp(long duration, double walkDistance, List<Leg> legs) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    /** startTime/endTime en milisegundos desde 1970 (UTC). */
    public record Leg(String mode, double duration, double distance, Lugar from, Lugar to,
                      Ruta route, String headsign, Geometria legGeometry, List<Lugar> intermediateStops,
                      long startTime, long endTime) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Lugar(String name) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Ruta(String shortName, String longName, String color) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Geometria(String points) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ErrorGql(String message) {}
}
