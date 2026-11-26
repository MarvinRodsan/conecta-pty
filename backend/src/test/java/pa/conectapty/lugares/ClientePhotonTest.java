package pa.conectapty.lugares;

import static org.assertj.core.api.Assertions.assertThat;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

class ClientePhotonTest {

    /** Igual que en application.yml (spring.jackson.deserialization). */
    private static final JsonMapper JSON_MAPPER = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES).build();

    // Misma forma que devuelve https://photon.komoot.io/api/ (GeoJSON); valores de prueba
    private static final String JSON = """
            {"type":"FeatureCollection","features":[
              {"geometry":{"type":"Point","coordinates":[-79.5321,9.0237]},
               "properties":{"name":"Universidad Tecnológica de Panamá","city":"Panamá","district":"Ancón",
                             "countrycode":"PA","osm_key":"amenity","osm_value":"university"}},
              {"geometry":{"type":"Point","coordinates":[-79.5321,9.0237]},
               "properties":{"name":"Universidad Tecnológica de Panamá","city":"Panamá","district":"Ancón",
                             "countrycode":"PA","osm_value":"university"}},
              {"geometry":{"type":"Point","coordinates":[-84.0,9.9]},
               "properties":{"name":"UTP Costa Rica","countrycode":"CR"}},
              {"geometry":{"type":"Point","coordinates":[-79.52,8.98]},
               "properties":{"street":"Calle 50","housenumber":"12","city":"Panamá","countrycode":"PA",
                             "osm_value":"house"}},
              {"geometry":{"type":"Point","coordinates":[-79.5,8.9]},
               "properties":{"countrycode":"PA"}}
            ]}
            """;

    @Test
    void convierteFiltraPanamaYQuitaRepetidos() throws Exception {
        ClientePhoton.Respuesta r = JSON_MAPPER.readValue(JSON, ClientePhoton.Respuesta.class);
        List<Lugar> lugares = ClientePhoton.convertir(r, 5);

        assertThat(lugares).hasSize(2);
        Lugar utp = lugares.get(0);
        assertThat(utp.nombre()).isEqualTo("Universidad Tecnológica de Panamá");
        assertThat(utp.detalle()).isEqualTo("Ancón, Panamá");
        assertThat(utp.lat()).isEqualTo(9.0237);
        assertThat(utp.lon()).isEqualTo(-79.5321);
        assertThat(utp.tipo()).isEqualTo("university");
        // Sin nombre: se arma con calle y número, y la calle no se repite en el detalle
        assertThat(lugares.get(1).nombre()).isEqualTo("Calle 50 12");
        assertThat(lugares.get(1).detalle()).isEqualTo("Calle 50, Panamá");
    }

    @Test
    void respetaElLimiteYToleraRespuestaVacia() throws Exception {
        ClientePhoton.Respuesta r = JSON_MAPPER.readValue(JSON, ClientePhoton.Respuesta.class);
        assertThat(ClientePhoton.convertir(r, 1)).hasSize(1);
        assertThat(ClientePhoton.convertir(null, 5)).isEmpty();
        assertThat(ClientePhoton.convertir(new ClientePhoton.Respuesta(null), 5)).isEmpty();
    }
}
