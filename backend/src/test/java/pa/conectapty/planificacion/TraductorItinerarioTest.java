package pa.conectapty.planificacion;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

class TraductorItinerarioTest {

    // Respuesta con la misma forma que devuelve OTP 2.x (valores inventados para la prueba)
    private static final String JSON = """
            {"data":{"plan":{"itineraries":[{
              "duration": 1500, "walkDistance": 420.4,
              "legs": [
                {"mode":"WALK","duration":300,"distance":400,"from":{"name":"Origin"},"to":{"name":"Vía Argentina"}},
                {"mode":"SUBWAY","duration":600,"distance":6000,"headsign":"Villa Zaita",
                 "from":{"name":"Vía Argentina"},"to":{"name":"San Miguelito"},
                 "route":{"shortName":"1","color":"E30613"},
                 "intermediateStops":[{"name":"A"},{"name":"B"},{"name":"C"}]},
                {"mode":"WALK","duration":10,"distance":8,"from":{"name":"San Miguelito"},"to":{"name":"San Miguelito"}},
                {"mode":"SUBWAY","duration":420,"distance":4000,"headsign":"Nuevo Tocumen",
                 "from":{"name":"San Miguelito"},"to":{"name":"Cincuentenario"},
                 "route":{"shortName":"2"},"intermediateStops":[]},
                {"mode":"WALK","duration":170,"distance":1234,"from":{"name":"Cincuentenario"},"to":{"name":"Destination"}}
              ]}]}}}
            """;

    @Test
    void traduceAInstruccionesSencillas() throws Exception {
        OtpRespuesta r = new ObjectMapper().readValue(JSON, OtpRespuesta.class);
        List<Itinerario.Opcion> opciones = TraductorItinerario.traducir(r);

        assertThat(opciones).hasSize(1);
        Itinerario.Opcion o = opciones.get(0);
        assertThat(o.duracionMin()).isEqualTo(25);
        assertThat(o.transbordos()).isEqualTo(1);
        assertThat(o.tramos()).hasSize(4); // la caminata de 8 m dentro de la estación se omite
        assertThat(o.tramos().get(0).instruccion()).isEqualTo("Camina 400 m hasta Vía Argentina.");
        assertThat(o.tramos().get(1).instruccion())
                .isEqualTo("En Vía Argentina, toma la Línea 1 hacia Villa Zaita y bájate en San Miguelito (4 paradas).");
        assertThat(o.tramos().get(2).instruccion()).endsWith("(1 parada).");
        assertThat(o.tramos().get(3).instruccion()).isEqualTo("Camina 1.2 km hasta tu destino.");
    }

    @Test
    void respuestaVaciaNoRevienta() throws Exception {
        OtpRespuesta r = new ObjectMapper().readValue("{\"data\":{\"plan\":null}}", OtpRespuesta.class);
        assertThat(TraductorItinerario.traducir(r)).isEmpty();
    }
}
