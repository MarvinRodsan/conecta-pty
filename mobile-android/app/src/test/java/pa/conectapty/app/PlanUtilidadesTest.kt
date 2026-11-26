package pa.conectapty.app

import androidx.compose.ui.graphics.Color
import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pa.conectapty.app.datos.Coordenada
import pa.conectapty.app.datos.Estacion
import pa.conectapty.app.datos.Punto
import pa.conectapty.app.datos.TipoPunto
import pa.conectapty.app.datos.remoto.OpcionDto
import pa.conectapty.app.datos.remoto.TramoDto
import pa.conectapty.app.ui.plan.Salida
import pa.conectapty.app.ui.plan.TrazoMapa
import pa.conectapty.app.ui.plan.comoParametro
import pa.conectapty.app.ui.plan.describir
import pa.conectapty.app.ui.plan.describirTramos
import pa.conectapty.app.ui.plan.distanciaLegible
import pa.conectapty.app.ui.plan.etiquetasOpciones
import pa.conectapty.app.ui.plan.fechaLegible
import pa.conectapty.app.ui.plan.geoJsonDe
import pa.conectapty.app.ui.plan.mismoLugar
import pa.conectapty.app.ui.plan.puntoDeUbicacion
import pa.conectapty.app.ui.plan.saludo
import pa.conectapty.app.ui.plan.textoSobre

class PlanUtilidadesTest {

    private val ahora = LocalDateTime.of(2026, 10, 28, 7, 45)

    @Test
    fun salidaComoParametroDeLaApi() {
        assertNull(Salida.Ahora.comoParametro(ahora))
        assertEquals("2026-10-28T08:15", Salida.En30Min.comoParametro(ahora))
        assertEquals("2026-11-02T06:00", Salida.A(LocalDateTime.of(2026, 11, 2, 6, 0)).comoParametro(ahora))
    }

    @Test
    fun describeLaSalidaEnEspanol() {
        assertEquals("Salir ahora", Salida.Ahora.describir(ahora))
        assertEquals("Salir hoy 18:30", Salida.A(LocalDateTime.of(2026, 10, 28, 18, 30)).describir(ahora))
        assertEquals("Salir mañana 07:00", Salida.A(LocalDateTime.of(2026, 10, 29, 7, 0)).describir(ahora))
        val otroDia = Salida.A(LocalDateTime.of(2026, 11, 4, 8, 0)).describir(ahora)
        assertTrue(otroDia, otroDia.startsWith("Salir ") && otroDia.contains("nov") && otroDia.endsWith("08:00"))
    }

    @Test
    fun fechaDeLosDatos() {
        assertEquals("28 oct 2026", fechaLegible("2026-10-28"))
        assertEquals("28 oct 2026", fechaLegible("2026-10-28T10:00:00Z"))
        assertEquals("no-es-fecha", fechaLegible("no-es-fecha")) // nunca inventar una fecha
    }

    @Test
    fun distanciasLegibles() {
        assertEquals("≈ 340 m", distanciaLegible(337))
        assertTrue(distanciaLegible(1250).matches(Regex("≈ 1[.,][23] km")))
    }

    @Test
    fun miUbicacionSugiereLaEstacionMasCercana() {
        val p = puntoDeUbicacion(Coordenada(8.9840, -79.5290), listOf(Estacion("b", "Iglesia del Carmen", 8.9850, -79.5280)))
        assertEquals(TipoPunto.UBICACION, p.tipo)
        assertEquals("Mi ubicación", p.nombre)
        assertTrue(p.detalle!!, p.detalle!!.startsWith("Estación más cercana: Iglesia del Carmen"))
        assertNull(puntoDeUbicacion(Coordenada(8.98, -79.52), emptyList()).detalle)
    }

    @Test
    fun detectaOrigenYDestinoIguales() {
        val a = Punto("Albrook", 8.9734, -79.5496, TipoPunto.ESTACION, estacionId = "A")
        assertTrue(mismoLugar(a, a.copy(lat = 8.0)))                       // misma estación
        assertTrue(mismoLugar(a, a.copy(estacionId = null, tipo = TipoPunto.LUGAR)))  // mismo punto
        assertFalse(mismoLugar(a, Punto("UTP", 9.0237, -79.5320, TipoPunto.LUGAR)))
    }

    @Test
    fun contrasteDelTextoSobreElColorDeLinea() {
        assertEquals(Color.White, textoSobre(Color(0xFF0B5C7A)))
        assertEquals(Color.Black, textoSobre(Color(0xFFFFD54F)))
    }

    @Test
    fun resumenParaLectorDePantalla() {
        val tramos = listOf(
            TramoDto("CAMINAR", "A", "B", duracionMin = 4, distanciaM = 300, instruccion = "Camina"),
            TramoDto("METRO", "B", "C", linea = "1", duracionMin = 10, distanciaM = 5000, instruccion = "Toma la L1"),
        )
        assertEquals("Caminas 4 minutos, luego Línea 1.", describirTramos(tramos))
    }

    @Test
    fun geoJsonConLongitudPrimero() {
        val json = geoJsonDe(listOf(TrazoMapa("METRO", "#123456", listOf(Coordenada(8.9, -79.5), Coordenada(9.0, -79.4)))))
        assertTrue(json, json.contains("[-79.5,8.9]"))
        assertTrue(json.contains("\"LineString\""))
        assertEquals(2, Regex("\"Point\"").findAll(json).count()) // inicio y fin
    }

    @Test
    fun hoyEnPanamaNoDependeDeLaZonaDelTelefono() {
        val hoy = pa.conectapty.app.ui.plan.ahoraEnPanama().toLocalDate()
        assertTrue(hoy.isAfter(LocalDate.of(2025, 1, 1)))
    }

    private fun opcion(min: Int, transbordos: Int, caminata: Int) = OpcionDto(min, transbordos, caminata, emptyList())

    @Test
    fun etiquetasSoloMarcanLoQueDistingueACadaOpcion() {
        val e = etiquetasOpciones(listOf(opcion(25, 1, 600), opcion(31, 0, 650), opcion(34, 1, 200)))
        assertEquals(listOf("Más rápida"), e[0])
        assertEquals(listOf("Directa"), e[1])
        assertEquals(listOf("Menos caminata"), e[2])
    }

    @Test
    fun sinEtiquetasConUnaOpcionOConEmpates() {
        assertEquals(listOf(emptyList<String>()), etiquetasOpciones(listOf(opcion(25, 0, 300))))
        // mismo tiempo, mismos transbordos y menos de 100 m de diferencia a pie: nada que destacar
        val e = etiquetasOpciones(listOf(opcion(25, 1, 300), opcion(25, 1, 350)))
        assertTrue(e.all { it.isEmpty() })
    }

    @Test
    fun menosTransbordosCuandoNingunaEsDirecta() {
        val e = etiquetasOpciones(listOf(opcion(20, 2, 400), opcion(26, 1, 400)))
        assertEquals(listOf("Más rápida"), e[0])
        assertEquals(listOf("Menos transbordos"), e[1])
    }

    @Test
    fun saludoSegunLaHoraDePanama() {
        assertEquals("Buenos días", saludo(5))
        assertEquals("Buenos días", saludo(11))
        assertEquals("Buenas tardes", saludo(12))
        assertEquals("Buenas tardes", saludo(18))
        assertEquals("Buenas noches", saludo(19))
        assertEquals("Buenas noches", saludo(0))
    }
}
