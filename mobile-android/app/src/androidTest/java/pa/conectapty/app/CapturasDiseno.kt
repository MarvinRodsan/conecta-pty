package pa.conectapty.app

import android.graphics.Bitmap
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import pa.conectapty.app.datos.Estacion
import pa.conectapty.app.datos.Punto
import pa.conectapty.app.datos.TipoPunto
import pa.conectapty.app.ui.componentes.Catalogo
import pa.conectapty.app.ui.plan.EstadoLugares
import pa.conectapty.app.ui.plan.EstadoPlan
import pa.conectapty.app.ui.plan.PantallaResultados
import pa.conectapty.app.ui.plan.TipoError
import pa.conectapty.app.ui.plan.PantallaInicio
import pa.conectapty.app.ui.plan.SelectorEstacion
import pa.conectapty.app.ui.plan.UiEstado
import pa.conectapty.app.ui.tema.ConectaTema
import java.io.File

/**
 * Capturas para revisar el diseño sin backend. Guarda PNG en files/capturas de la app:
 *   adb exec-out run-as pa.conectapty.app cat files/capturas/<nombre>.png > <nombre>.png
 */
@RunWith(AndroidJUnit4::class)
class CapturasDiseno {

    @get:Rule
    val regla = createComposeRule()

    private val albrook = Punto("Albrook", 8.9734, -79.5496, TipoPunto.ESTACION, estacionId = "A")
    private val utp = Punto("UTP", 9.0237, -79.5320, TipoPunto.LUGAR, detalle = "Víctor Manuel Pérez")

    private fun guardar(nombre: String) {
        val dir = File(InstrumentationRegistry.getInstrumentation().targetContext.filesDir, "capturas").apply { mkdirs() }
        val bmp = regla.onRoot().captureToImage().asAndroidBitmap()
        File(dir, "$nombre.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun conTema(oscuro: Boolean, contenido: @Composable () -> Unit) = regla.setContent {
        ConectaTema(oscuro = oscuro) { contenido() }
    }

    @Test
    fun catalogoClaro() = catalogo(false)

    @Test
    fun catalogoOscuro() = catalogo(true)

    /** El catálogo es más alto que la pantalla: dos capturas, arriba y abajo. */
    private fun catalogo(oscuro: Boolean) {
        val scroll = ScrollState(0)
        conTema(oscuro) { Box(Modifier.verticalScroll(scroll)) { Catalogo() } }
        val sufijo = if (oscuro) "oscuro" else "claro"
        guardar("catalogo-$sufijo-1")
        regla.runOnUiThread { runBlocking { scroll.scrollTo(scroll.maxValue) } }
        regla.waitForIdle()
        guardar("catalogo-$sufijo-2")
    }

    @Test
    fun inicioClaro() = inicio("claro", false, UiEstado(origen = albrook, destino = utp))

    @Test
    fun inicioOscuro() = inicio("oscuro", true, UiEstado(origen = albrook, destino = utp))

    @Test
    fun inicioVacio() = inicio("vacio", false, UiEstado())

    @Test
    fun inicioFavoritosSinConexion() = inicio(
        "favoritos-sin-conexion", false, UiEstado(destino = utp, catalogoSinConexion = true),
        listOf(Estacion("A", "Albrook", 8.97, -79.55), Estacion("B", "Iglesia del Carmen", 8.99, -79.52), Estacion("C", "San Miguelito", 9.03, -79.50)),
    )

    private fun inicio(nombre: String, oscuro: Boolean, ui: UiEstado, favoritos: List<Estacion> = emptyList()) {
        conTema(oscuro) {
            PantallaInicio(
                ui = ui, favoritos = favoritos, hayCatalogo = true,
                onElegir = {}, onUsarUbicacion = {}, onIntercambiar = {}, onFavorito = {},
                onSalida = {}, onBuscar = {}, onReintentarCatalogo = {}, onReportar = {},
            )
        }
        guardar("inicio-$nombre")
    }

    // --- Selector de estación ---

    private val estaciones = listOf(
        Estacion("A", "Albrook", 8.97, -79.55), Estacion("B", "Iglesia del Carmen", 8.99, -79.52),
        Estacion("C", "San Miguelito", 9.03, -79.50), Estacion("D", "Vía Argentina", 8.99, -79.53),
        Estacion("E", "5 de Mayo", 8.96, -79.54), Estacion("F", "Lotería", 8.97, -79.53),
    )

    private fun selector(nombre: String, oscuro: Boolean, lugares: EstadoLugares, consulta: String? = null, catalogo: List<Estacion> = estaciones) {
        conTema(oscuro) {
            SelectorEstacion(
                titulo = "¿A dónde vas?", estaciones = catalogo, favoritos = listOf(estaciones[0], estaciones[3]),
                lugares = lugares, buscandoUbicacion = false, onConsulta = {}, onElegir = {}, onUsarUbicacion = {},
                onAlternarFavorito = {}, onVolver = {}, seleccionado = estaciones[3].comoPunto(), esOrigen = false,
            )
        }
        consulta?.let { regla.onNodeWithText("Estación, dirección o lugar").performTextInput(it) }
        regla.waitForIdle()
        guardar("selector-$nombre")
    }

    @Test
    fun selectorClaro() = selector("claro", false, EstadoLugares.Inactivo)

    @Test
    fun selectorOscuro() = selector("oscuro", true, EstadoLugares.Inactivo)

    @Test
    fun selectorBuscandoLugares() = selector(
        "lugares", false,
        EstadoLugares.Listo(listOf(
            Punto("Universidad Tecnológica de Panamá", 9.02, -79.53, TipoPunto.LUGAR, detalle = "Víctor Manuel Pérez, Panamá"),
            Punto("UTP Centro Regional", 9.01, -79.52, TipoPunto.LUGAR, detalle = "Calidonia, Panamá"),
        )),
        consulta = "utp",
    )

    @Test
    fun selectorErrorLugares() = selector("error", false, EstadoLugares.Error("Sin conexión con el servidor. Revisa tu internet e inténtalo de nuevo."), consulta = "xyz")

    // --- Resultados ---

    private fun resultados(nombre: String, oscuro: Boolean, plan: EstadoPlan) {
        conTema(oscuro) {
            PantallaResultados(
                ui = UiEstado(origen = albrook, destino = utp, plan = plan),
                onVerDetalle = {}, onReintentar = {}, onVolver = {}, onReportar = {},
            )
        }
        regla.waitForIdle()
        guardar("resultados-$nombre")
    }

    @Test
    fun resultadosClaro() = resultados("claro", false, EstadoPlan.Listo(planDeEjemplo()))

    @Test
    fun resultadosOscuro() = resultados("oscuro", true, EstadoPlan.Listo(planDeEjemplo()))

    @Test
    fun resultadosCargando() {
        regla.mainClock.autoAdvance = false // el esqueleto late sin fin: sin esto la captura nunca queda "quieta"
        conTema(false) {
            PantallaResultados(
                ui = UiEstado(origen = albrook, destino = utp, plan = EstadoPlan.Cargando),
                onVerDetalle = {}, onReintentar = {}, onVolver = {}, onReportar = {},
            )
        }
        regla.mainClock.advanceTimeBy(500)
        guardar("resultados-cargando")
    }

    @Test
    fun resultadosSinConexion() = resultados(
        "sin-conexion", false,
        EstadoPlan.Error("Sin conexión con el servidor. Revisa tu internet e inténtalo de nuevo.", TipoError.SIN_CONEXION),
    )

    @Test
    fun resultadosSinRutas() = resultados(
        "sin-rutas", false,
        EstadoPlan.Listo(planDeEjemplo(aviso = "El Metro no opera a esta hora. Abre a las 5:00.", opciones = false)),
    )

    @Test
    fun selectorSinCatalogo() = selector("sin-catalogo", false, EstadoLugares.Inactivo, catalogo = emptyList())
}
