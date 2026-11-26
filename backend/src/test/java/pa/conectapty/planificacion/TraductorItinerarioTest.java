package pa.conectapty.planificacion;

import static org.assertj.core.api.Assertions.assertThat;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

class TraductorItinerarioTest {

    /** Igual que en application.yml (spring.jackson.deserialization). */
    private static final JsonMapper JSON_MAPPER = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES).build();

    // Respuesta con la misma forma que devuelve OTP 2.x (valores inventados para la prueba)
    private static final String JSON = """
            {"data":{"plan":{"itineraries":[
             {"duration": 4688, "walkDistance": 6170,
              "legs": [{"mode":"WALK","duration":4688,"distance":6170,"from":{"name":"Origin"},"to":{"name":"Destination"}}]},
             {"duration": 1500, "walkDistance": 420.4,
              "legs": [
                {"mode":"WALK","duration":300,"distance":400,"startTime":1793019600000,
                 "from":{"name":"Origin"},"to":{"name":"Vía Argentina"}},
                {"mode":"SUBWAY","duration":600,"distance":6000,"headsign":"Villa Zaita",
                 "from":{"name":"Vía Argentina"},"to":{"name":"San Miguelito"},
                 "route":{"shortName":"1","color":"E30613"},
                 "intermediateStops":[{"name":"A"},{"name":"B"},{"name":"C"}]},
                {"mode":"WALK","duration":10,"distance":8,"from":{"name":"San Miguelito"},"to":{"name":"San Miguelito"}},
                {"mode":"SUBWAY","duration":420,"distance":4000,"headsign":"Nuevo Tocumen",
                 "from":{"name":"San Miguelito"},"to":{"name":"Cincuentenario"},
                 "route":{"shortName":"2"},"intermediateStops":[]},
                {"mode":"WALK","duration":170,"distance":1234,"endTime":1793021100000,
                 "from":{"name":"Cincuentenario"},"to":{"name":"Destination"}}
              ]}]}}}
            """;

    @Test
    void traduceAInstruccionesSencillas() throws Exception {
        OtpRespuesta r = JSON_MAPPER.readValue(JSON, OtpRespuesta.class);
        List<Itinerario.Opcion> opciones = TraductorItinerario.traducir(r);

        assertThat(opciones).hasSize(2);
        Itinerario.Opcion o = opciones.get(0);             // la opción con Metro va primero
        assertThat(o.usaMetro()).isTrue();
        assertThat(opciones.get(1).usaMetro()).isFalse();
        assertThat(o.sale()).isEqualTo("08:00");            // hora de Panamá (UTC-5)
        assertThat(o.llega()).isEqualTo("08:25");
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
        OtpRespuesta r = JSON_MAPPER.readValue("{\"data\":{\"plan\":null}}", OtpRespuesta.class);
        assertThat(TraductorItinerario.traducir(r)).isEmpty();
    }
}
