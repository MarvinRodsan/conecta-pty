package pa.conectapty.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pa.conectapty.app.datos.Coordenada
import pa.conectapty.app.datos.Estacion
import pa.conectapty.app.datos.decodificarPolilinea
import pa.conectapty.app.datos.distanciaM
import pa.conectapty.app.datos.enPanama
import pa.conectapty.app.datos.estacionMasCercana

class GeoTest {

    @Test
    fun decodificaElEjemploOficialDeGoogle() {
        val puntos = decodificarPolilinea("_p~iF~ps|U_ulLnnqC_mqNvxq`@")
        assertEquals(3, puntos.size)
        assertEquals(38.5, puntos[0].lat, 1e-6)
        assertEquals(-120.2, puntos[0].lon, 1e-6)
        assertEquals(40.7, puntos[1].lat, 1e-6)
        assertEquals(-120.95, puntos[1].lon, 1e-6)
        assertEquals(43.252, puntos[2].lat, 1e-6)
        assertEquals(-126.453, puntos[2].lon, 1e-6)
    }

    @Test
    fun polilineaVaciaOCortadaNoRevienta() {
        assertTrue(decodificarPolilinea("").isEmpty())
        // Solo la latitud del primer punto: se descarta en vez de lanzar una excepción
        assertTrue(decodificarPolilinea("_p~iF").isEmpty())
    }

    @Test
    fun distanciaEntreDosEstacionesConocidas() {
        // Albrook y 5 de Mayo están a unos 1,8 km en línea recta
        val albrook = Coordenada(8.9734, -79.5496)
        val cincoDeMayo = Coordenada(8.9618, -79.5386)
        val d = distanciaM(albrook, cincoDeMayo)
        assertTrue("distancia $d", d in 1_500.0..2_500.0)
    }

    @Test
    fun eligeLaEstacionMasCercana() {
        val estaciones = listOf(
            Estacion("a", "Albrook", 8.9734, -79.5496),
            Estacion("b", "Iglesia del Carmen", 8.9850, -79.5280),
            Estacion("c", "San Miguelito", 9.0310, -79.5050),
        )
        val (e, metros) = estacionMasCercana(Coordenada(8.9840, -79.5290), estaciones)!!
        assertEquals("Iglesia del Carmen", e.nombre)
        assertTrue(metros < 200)
        assertNull(estacionMasCercana(Coordenada(9.0, -79.5), emptyList()))
    }

    @Test
    fun soloAceptaUbicacionesDentroDePanama() {
        assertTrue(enPanama(Coordenada(8.98, -79.52)))
        assertFalse(enPanama(Coordenada(37.42, -122.08))) // emulador por defecto (California)
    }
}
