package pa.conectapty.lugares;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import pa.conectapty.comun.ConectaProperties;
import pa.conectapty.comun.ServicioNoDisponibleException;

/**
 * Único punto de contacto con el geocodificador Photon (https://photon.komoot.io),
 * que busca sobre datos de OpenStreetMap sin API key. Si se cambia por Nominatim
 * o un Photon propio, solo se toca esta clase.
 */
@Component
public class ClientePhoton {

    private static final Logger log = LoggerFactory.getLogger(ClientePhoton.class);
    // Panamá continental (lon mín, lat mín, lon máx, lat máx), como exige Photon
    static final String BBOX_PANAMA = "-83.1,7.0,-77.1,10.0";
    // Sesgo hacia la Ciudad de Panamá: "Albrook" encuentra primero el de la capital
    static final double LAT_CIUDAD = 8.98, LON_CIUDAD = -79.52;

    private final RestClient http;
    private final boolean habilitado;

    public ClientePhoton(RestClient.Builder builder, ConectaProperties props) {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(Duration.ofSeconds(3));
        f.setReadTimeout(Duration.ofMillis(props.geocodificador().timeoutMs()));
        this.habilitado = props.geocodificador().habilitado();
        this.http = builder.baseUrl(props.geocodificador().url()).requestFactory(f)
                // Política de uso de Photon: identificarse
                .defaultHeader("User-Agent", "ConectaPTY/0.2 (planificador de transporte de Panama)")
                .build();
    }

    public List<Lugar> buscar(String texto, int limite) {
        if (!habilitado) {
            throw new ServicioNoDisponibleException("La búsqueda de direcciones está desactivada en este servidor.", null);
        }
        try {
            Respuesta r = http.get()
                    .uri(u -> u.path("/api/")
                            .queryParam("q", texto)
                            .queryParam("limit", limite * 2)    // se filtran los que no son de Panamá
                            .queryParam("bbox", BBOX_PANAMA)
                            .queryParam("lat", LAT_CIUDAD)
                            .queryParam("lon", LON_CIUDAD)
                            .build())
                    .retrieve()
                    .body(Respuesta.class);
            return convertir(r, limite);
        } catch (RestClientException e) {
            log.warn("El geocodificador no respondió: {}", e.getMessage());
            throw new ServicioNoDisponibleException(
                    "El buscador de direcciones no está disponible. Elige una estación de la lista.", e);
        }
    }

    /** GeoJSON de Photon -> lugares de Panamá, sin repetidos y con un nombre legible. */
    static List<Lugar> convertir(Respuesta r, int limite) {
        if (r == null || r.features() == null) return List.of();
        List<Lugar> out = new ArrayList<>();
        Set<String> vistos = new LinkedHashSet<>();
        for (Feature f : r.features()) {
            Propiedades p = f.properties();
            if (p == null || f.geometry() == null || f.geometry().coordinates() == null
                    || f.geometry().coordinates().size() < 2) continue;
            if (p.countrycode() != null && !"PA".equalsIgnoreCase(p.countrycode())) continue;
            String nombre = nombre(p);
            if (nombre == null) continue;
            String detalle = detalle(p, nombre);
            if (!vistos.add(nombre + "|" + detalle)) continue;
            double lon = f.geometry().coordinates().get(0);
            double lat = f.geometry().coordinates().get(1);
            out.add(new Lugar(nombre, detalle, lat, lon, p.osmValue()));
            if (out.size() == limite) break;
        }
        return out;
    }

    private static String nombre(Propiedades p) {
        if (p.name() != null && !p.name().isBlank()) return p.name();
        if (p.street() != null) return p.housenumber() == null ? p.street() : p.street() + " " + p.housenumber();
        return null;
    }

    private static String detalle(Propiedades p, String nombre) {
        Set<String> partes = new LinkedHashSet<>();
        for (String s : new String[] {p.street(), p.district(), p.city(), p.state()}) {
            if (s != null && !s.isBlank() && !Objects.equals(s, nombre)) partes.add(s);
        }
        return String.join(", ", partes);
    }

    // --- Forma de la respuesta de Photon (solo lo que usamos)
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Respuesta(List<Feature> features) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Feature(Geometria geometry, Propiedades properties) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Geometria(List<Double> coordinates) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Propiedades(String name, String street, String housenumber, String district, String city,
                       String state, String countrycode,
                       @com.fasterxml.jackson.annotation.JsonProperty("osm_value") String osmValue) {}
}
