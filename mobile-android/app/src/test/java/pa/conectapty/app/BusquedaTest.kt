package pa.conectapty.app

import org.junit.Assert.assertEquals
import org.junit.Test
import pa.conectapty.app.datos.Estacion
import pa.conectapty.app.datos.filtrarEstaciones
import pa.conectapty.app.datos.normalizar

class BusquedaTest {

    private val estaciones = listOf(
        Estacion("1", "Vía Argentina", 0.0, 0.0),
        Estacion("2", "San Miguelito", 0.0, 0.0),
        Estacion("3", "Iglesia del Carmen", 0.0, 0.0),
        Estacion("4", "Villa Zaita", 0.0, 0.0),
    )

    @Test
    fun normalizaTildesYMayusculas() {
        assertEquals("via argentina", normalizar("  Vía ARGENTINA "))
    }

    @Test
    fun buscaSinTildes() {
        assertEquals(listOf("Vía Argentina"), filtrarEstaciones(estaciones, "via arg").map { it.nombre })
    }

    @Test
    fun priorizaLasQueEmpiezanConElTexto() {
        // "vi" aparece en "Vía Argentina" y "Villa Zaita" (al inicio) -> ambas primero, conservando orden
        assertEquals(listOf("Vía Argentina", "Villa Zaita"), filtrarEstaciones(estaciones, "vi").map { it.nombre })
    }

    @Test
    fun consultaVaciaDevuelveTodo() {
        assertEquals(4, filtrarEstaciones(estaciones, "  ").size)
    }
}
