package pa.conectapty;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import pa.conectapty.comun.ConectaProperties;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pa.conectapty.comun.ServicioNoDisponibleException;
import pa.conectapty.lugares.Lugar;
import pa.conectapty.lugares.LugarControlador;
import pa.conectapty.lugares.ServicioLugares;
import pa.conectapty.planificacion.Itinerario;
import pa.conectapty.planificacion.PlanControlador;
import pa.conectapty.planificacion.ServicioPlanificacion;

/**
 * Capa web sin base de datos ni OTP: validaciones, errores en español,
 * cabeceras de seguridad y límite de peticiones. No necesita Docker.
 */
@WebMvcTest(controllers = {PlanControlador.class, LugarControlador.class})
@Import(ControladoresWebTest.Propiedades.class)
@TestPropertySource(properties = {"conecta.limites.plan=3", "conecta.limites.lugares=100"})
class ControladoresWebTest {

    /** El corte web no escanea @ConfigurationProperties: el filtro de seguridad las necesita. */
    @TestConfiguration
    @EnableConfigurationProperties(ConectaProperties.class)
    static class Propiedades {
        /** @EnableCaching está en la aplicación; en Spring Boot 4 el corte web no crea la caché. */
        @Bean CacheManager cacheManager() { return new NoOpCacheManager(); }
    }

    @Autowired MockMvc mvc;
    @MockitoBean ServicioPlanificacion planificacion;
    @MockitoBean ServicioLugares lugares;

    private static final Itinerario.Plan VACIO = new Itinerario.Plan("2026-11-02T08:00", List.of(),
            Itinerario.CalidadDatos.pilotoActual(), null);

    @Test
    void planRedondeaCoordenadasYAgregaCabecerasDeSeguridad() throws Exception {
        when(planificacion.planificar(anyDouble(), anyDouble(), anyDouble(), anyDouble(), any())).thenReturn(VACIO);
        String salida = LocalDateTime.now().plusDays(1).withHour(8).withMinute(0).withSecond(0).withNano(0).toString();
        mvc.perform(get("/api/v1/plan").remoteAddress("10.0.0.1")
                        .param("desdeLat", "9.012345678").param("desdeLon", "-79.5").param("hastaLat", "9.1")
                        .param("hastaLon", "-79.4").param("salida", salida))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.calidad.tiempos").value("estimado"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'"));
        verify(planificacion).planificar(eq(9.0123), eq(-79.5), eq(9.1), eq(-79.4), eq(LocalDateTime.parse(salida)));
    }

    @Test
    void planFueraDePanamaDa400EnEspanol() throws Exception {
        mvc.perform(get("/api/v1/plan").remoteAddress("10.0.0.2")
                        .param("desdeLat", "40.7").param("desdeLon", "-74.0")
                        .param("hastaLat", "9.1").param("hastaLon", "-79.4"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Petición inválida"))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("Panamá")));
    }

    @Test
    void planConHoraLejanaOFaltandoParametrosDa400() throws Exception {
        mvc.perform(get("/api/v1/plan").remoteAddress("10.0.0.3")
                        .param("desdeLat", "9.0").param("desdeLon", "-79.5").param("hastaLat", "9.1")
                        .param("hastaLon", "-79.4").param("salida", "2099-01-01T08:00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("60 días")));
        mvc.perform(get("/api/v1/plan").remoteAddress("10.0.0.3").param("desdeLat", "9.0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("Falta el parámetro")));
    }

    @Test
    void limiteDePlanDevuelve429ConRetryAfter() throws Exception {
        when(planificacion.planificar(anyDouble(), anyDouble(), anyDouble(), anyDouble(), any())).thenReturn(VACIO);
        for (int i = 0; i < 3; i++) {
            mvc.perform(get("/api/v1/plan").remoteAddress("10.0.0.9")
                            .param("desdeLat", "9.0").param("desdeLon", "-79.5")
                            .param("hastaLat", "9.1").param("hastaLon", "-79.4"))
                    .andExpect(status().isOk());
        }
        mvc.perform(get("/api/v1/plan").remoteAddress("10.0.0.9")
                        .param("desdeLat", "9.0").param("desdeLon", "-79.5")
                        .param("hastaLat", "9.1").param("hastaLon", "-79.4"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
    }

    @Test
    void lugaresNormalizaLaBusqueda() throws Exception {
        when(lugares.buscar("albrook mall", 5)).thenReturn(
                List.of(new Lugar("Albrook Mall", "Ancón, Panamá", 8.9737, -79.5503, "mall")));
        mvc.perform(get("/api/v1/lugares").remoteAddress("10.0.1.1").param("q", "  Albrook   MALL "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Albrook Mall"))
                .andExpect(jsonPath("$[0].lat").value(8.9737));
    }

    @Test
    void lugaresValidaYReportaServicioCaido() throws Exception {
        mvc.perform(get("/api/v1/lugares").remoteAddress("10.0.1.2").param("q", "ab"))
                .andExpect(status().isBadRequest());
        when(lugares.buscar(anyString(), anyInt()))
                .thenThrow(new ServicioNoDisponibleException("El buscador de direcciones no está disponible.", null));
        mvc.perform(get("/api/v1/lugares").remoteAddress("10.0.1.2").param("q", "utp"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.detail").value("El buscador de direcciones no está disponible."));
    }
}
