package pa.conectapty;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import pa.conectapty.paradas.ImportadorGtfs;

/**
 * De punta a punta contra PostgreSQL + PostGIS reales (la misma imagen que docker-compose):
 * migraciones Flyway, importación del GTFS, búsquedas espaciales y sin tildes, salud y reportes.
 *
 * Necesita Docker. Si no está corriendo, JUnit marca estas pruebas como "skipped" en vez de fallar;
 * en GitHub Actions siempre corren.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class IntegracionBdTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer DB = new PostgreSQLContainer(
            DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"));

    static final Path GTFS = crearGtfs();

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry r) {
        r.add("conecta.gtfs.path", GTFS::toString);
        r.add("conecta.otp.url", () -> "http://localhost:1");   // OTP no se usa en estas pruebas
        r.add("conecta.otp.calentar", () -> "false");
        r.add("conecta.geocodificador.habilitado", () -> "false");
    }

    @Autowired MockMvc mvc;
    @Autowired JdbcClient jdbc;
    @Autowired ImportadorGtfs importador;

    @Test
    void importaEstacionesYEsIdempotente() throws Exception {
        mvc.perform(get("/api/v1/paradas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))                 // solo estaciones, no andenes
                .andExpect(jsonPath("$[0].accesible").value("desconocido"));

        importador.run(new DefaultApplicationArguments());                  // mismo archivo otra vez
        long publicadas = jdbc.sql("SELECT count(*) FROM importacion_datos WHERE estado = 'publicada'")
                .query(Long.class).single();
        assertThat(publicadas).isEqualTo(1);
    }

    @Test
    void cercanasOrdenaPorDistanciaConPostgis() throws Exception {
        // Junto a Vía Argentina: primero ella, luego Iglesia del Carmen (~600 m)
        mvc.perform(get("/api/v1/paradas/cercanas").param("lat", "8.9905").param("lon", "-79.5245").param("radio", "2000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Vía Argentina"))
                .andExpect(jsonPath("$[0].distanciaM").value(org.hamcrest.Matchers.lessThan(100)))
                .andExpect(jsonPath("$[1].nombre").value("Iglesia del Carmen"))
                .andExpect(jsonPath("$.length()").value(2));                 // San Miguelito queda fuera del radio
    }

    @Test
    void buscaSinTildesNiMayusculas() throws Exception {
        mvc.perform(get("/api/v1/paradas/buscar").param("q", "VIA ARG"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("EST_via-argentina"));
    }

    @Test
    void saludInformaFechaYFuenteDeLosDatos() throws Exception {
        mvc.perform(get("/api/v1/salud"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ok"))
                .andExpect(jsonPath("$.datos.actualizadoEl").value("2026-10-28"))
                .andExpect(jsonPath("$.datos.fuente").value("OpenStreetMap"))
                .andExpect(jsonPath("$.datos.licencia").value("ODbL 1.0"));
    }

    @Test
    void guardaReporteAnonimoYRechazaInvalidos() throws Exception {
        mvc.perform(post("/api/v1/reportes").contentType(MediaType.APPLICATION_JSON).content("""
                        {"tipo":"ESTACION","mensaje":"La entrada sur está cerrada\\u0007 por obras",
                         "paradaId":"EST_via-argentina","versionApp":"0.2.0"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber());
        String guardado = jdbc.sql("SELECT mensaje FROM reporte ORDER BY id DESC LIMIT 1").query(String.class).single();
        assertThat(guardado).isEqualTo("La entrada sur está cerrada por obras");   // sin caracteres de control

        mvc.perform(post("/api/v1/reportes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"RUTA\",\"mensaje\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("mensaje")));
        mvc.perform(post("/api/v1/reportes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"HACKEO\",\"mensaje\":\"hola mundo\"}"))
                .andExpect(status().isBadRequest());
    }

    /** GTFS mínimo con 3 estaciones reales del Metro (coordenadas aproximadas) y un andén. */
    private static Path crearGtfs() {
        try {
            Path zip = Files.createTempFile("gtfs-prueba", ".zip");
            try (ZipOutputStream z = new ZipOutputStream(Files.newOutputStream(zip))) {
                escribir(z, "feed_info.txt", """
                        feed_publisher_name,feed_publisher_url,feed_lang,feed_version
                        Conecta PTY,https://github.com/conecta-pty,es,2026-10-28
                        """);
                escribir(z, "stops.txt", """
                        stop_id,stop_name,stop_lat,stop_lon,location_type,parent_station,wheelchair_boarding
                        EST_via-argentina,Vía Argentina,8.990500,-79.524500,1,,0
                        EST_iglesia-del-carmen,Iglesia del Carmen,8.986700,-79.529000,1,,0
                        EST_san-miguelito,San Miguelito,9.028900,-79.506800,1,,0
                        N1,Vía Argentina,8.990600,-79.524400,0,EST_via-argentina,0
                        """);
            }
            zip.toFile().deleteOnExit();
            return zip;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void escribir(ZipOutputStream z, String nombre, String contenido) throws IOException {
        z.putNextEntry(new ZipEntry(nombre));
        OutputStream o = z;
        o.write(contenido.getBytes(StandardCharsets.UTF_8));
        z.closeEntry();
    }
}
