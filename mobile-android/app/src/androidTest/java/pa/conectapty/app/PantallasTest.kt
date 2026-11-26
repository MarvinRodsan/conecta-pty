package pa.conectapty.app
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import pa.conectapty.app.datos.Estacion
import pa.conectapty.app.datos.Punto
import pa.conectapty.app.datos.TipoPunto
import pa.conectapty.app.datos.remoto.CalidadDto
import pa.conectapty.app.datos.remoto.OpcionDto
import pa.conectapty.app.datos.remoto.PlanDto
import pa.conectapty.app.datos.remoto.TramoDto
import pa.conectapty.app.ui.plan.EstadoEnvio
import pa.conectapty.app.ui.plan.EstadoLugares
import pa.conectapty.app.ui.plan.EstadoPlan
import pa.conectapty.app.ui.plan.PantallaResultados
import pa.conectapty.app.ui.plan.TipoError
import pa.conectapty.app.ui.plan.PantallaInicio
import pa.conectapty.app.ui.plan.PantallaReporte
import pa.conectapty.app.ui.plan.Salida
import pa.conectapty.app.ui.plan.SelectorEstacion
import pa.conectapty.app.ui.plan.TipoReporte
import pa.conectapty.app.ui.plan.UiEstado
import pa.conectapty.app.ui.tema.ConectaTema

/**
 * Pruebas de pantallas en un teléfono o emulador:
 *   ./gradlew connectedDebugAndroidTest
 * Buscan por texto y por descripción de accesibilidad: si TalkBack no puede encontrar algo, la prueba tampoco.
 */
@RunWith(AndroidJUnit4::class)
class PantallasTest {

    @get:Rule
    val regla = createComposeRule()

    private val albrook = Punto("Albrook", 8.9734, -79.5496, TipoPunto.ESTACION, estacionId = "A")
    private val utp = Punto("UTP", 9.0237, -79.5320, TipoPunto.LUGAR, detalle = "Víctor Manuel Pérez")

    private fun inicio(ui: UiEstado, onSalida: (Salida) -> Unit = {}, onBuscar: () -> Unit = {}) = regla.setContent {
        ConectaTema {
            PantallaInicio(
                ui = ui, favoritos = emptyList(), hayCatalogo = true,
                onElegir = {}, onUsarUbicacion = {}, onIntercambiar = {}, onFavorito = {},
                onSalida = onSalida, onBuscar = onBuscar, onReintentarCatalogo = {}, onReportar = {},
            )
        }
    }

    // --- Resultados ---

    private fun resultados(
        plan: EstadoPlan,
        onVerDetalle: (Int) -> Unit = {},
        onReintentar: () -> Unit = {},
        onVolver: () -> Unit = {},
    ) = regla.setContent {
        ConectaTema {
            PantallaResultados(
                ui = UiEstado(origen = albrook, destino = utp, plan = plan),
                onVerDetalle = onVerDetalle, onReintentar = onReintentar, onVolver = onVolver, onReportar = {},
            )
        }
    }

    @Test
    fun cadaOpcionSeLeeEnteraYAbreSuDetalle() {
        var abierta = -1
        resultados(EstadoPlan.Listo(planDeEjemplo()), onVerDetalle = { abierta = it })
        regla.onNodeWithText("2 opciones").assertExists()
        regla.onNode(hasContentDescription("Opción 2, directa", substring = true)).performClick()
        assertEquals(1, abierta)
        regla.onNode(hasContentDescription("Opción 1, más rápida", substring = true)).assertExists()
    }

    @Test
    fun sinConexionOfreceReintentar() {
        var reintentos = 0
        resultados(EstadoPlan.Error("Sin conexión con el servidor.", TipoError.SIN_CONEXION), onReintentar = { reintentos++ })
        regla.onNodeWithText("Sin conexión").assertExists()
        regla.onNodeWithText("Reintentar").performClick()
        assertEquals(1, reintentos)
    }

    @Test
    fun mismoLugarLlevaACambiarLaBusqueda() {
        var volvio = false
        resultados(EstadoPlan.Error("El origen y el destino son el mismo lugar.", TipoError.MISMO_LUGAR), onVolver = { volvio = true })
        regla.onNodeWithText("Reintentar").assertDoesNotExist()
        regla.onNodeWithText("Cambiar origen o destino").performClick()
        assertEquals(true, volvio)
    }

    @Test
    fun sinOpcionesMuestraElAvisoYCambiarBusqueda() {
        resultados(EstadoPlan.Listo(planDeEjemplo(aviso = "El Metro no opera a esta hora.", opciones = false)))
        regla.onNodeWithText("El Metro no opera a esta hora.").assertExists()
        regla.onNodeWithText("No hay rutas en Metro").assertExists()
        regla.onNodeWithText("Cambiar búsqueda").assertExists()
    }

    @Test
    fun buscarRutaSoloSeActivaConOrigenYDestino() {
        inicio(UiEstado(origen = albrook))
        regla.onNodeWithText("Buscar ruta").assertIsNotEnabled()
    }

    @Test
    fun conOrigenYDestinoSePuedeBuscar() {
        var buscado = false
        inicio(UiEstado(origen = albrook, destino = utp), onBuscar = { buscado = true })
        regla.onNodeWithText("Buscar ruta").assertIsEnabled().performClick()
        assertEquals(true, buscado)
    }

    @Test
    fun losCamposSeLeenConSuDetalle() {
        inicio(UiEstado(origen = albrook, destino = utp))
        regla.onNodeWithContentDescription("Hasta: UTP, Víctor Manuel Pérez. Toca para cambiar.").assertExists()
    }

    @Test
    fun elegirSalidaEn30Minutos() {
        var elegida: Salida? = null
        inicio(UiEstado(), onSalida = { elegida = it })
        regla.onNodeWithText("En 30 min").performClick()
        assertEquals(Salida.En30Min, elegida)
    }

    @Test
    fun elReporteExigeUnMensajeMinimo() {
        var enviado: Triple<TipoReporte, String, Boolean>? = null
        regla.setContent {
            ConectaTema {
                PantallaReporte(EstadoEnvio.Editando, hayRuta = true, onEnviar = { t, m, r -> enviado = Triple(t, m, r) }, onVolver = {})
            }
        }
        regla.onNodeWithText("Enviar reporte").assertIsNotEnabled()
        regla.onNodeWithText("Cuéntanos qué viste").performTextInput("La entrada está en la otra calle")
        regla.onNodeWithText("Enviar reporte").assertIsEnabled().performClick()
        assertEquals(TipoReporte.RUTA, enviado?.first)
        assertEquals(true, enviado?.third)
    }

    @Test
    fun reporteEnviadoMuestraElAgradecimiento() {
        regla.setContent {
            ConectaTema { PantallaReporte(EstadoEnvio.Enviado("¡Gracias!"), hayRuta = false, onEnviar = { _, _, _ -> }, onVolver = {}) }
        }
        regla.onNodeWithText("¡Gracias!").assertExists()
    }

    // --- Selector de estación ---

    private val estaciones = listOf(
        Estacion("A", "Albrook", 8.9734, -79.5496),
        Estacion("B", "Iglesia del Carmen", 8.9937, -79.5263),
        Estacion("C", "San Miguelito", 9.0322, -79.5049),
    )

    private fun selector(
        favoritos: List<Estacion> = emptyList(),
        seleccionado: Punto? = null,
        onElegir: (Punto) -> Unit = {},
        onAlternarFavorito: (Estacion) -> Unit = {},
    ) = regla.setContent {
        ConectaTema {
            SelectorEstacion(
                titulo = "¿A dónde vas?", estaciones = estaciones, favoritos = favoritos, lugares = EstadoLugares.Inactivo,
                buscandoUbicacion = false, onConsulta = {}, onElegir = onElegir, onUsarUbicacion = {},
                onAlternarFavorito = onAlternarFavorito, onVolver = {}, seleccionado = seleccionado, esOrigen = false,
            )
        }
    }

    @Test
    fun tocarUnaEstacionLaElige() {
        var elegido: Punto? = null
        selector(onElegir = { elegido = it })
        regla.onNodeWithText("San Miguelito").performClick()
        assertEquals("C", elegido?.estacionId)
    }

    @Test
    fun laEstrellaGuardaYQuitaFavoritos() {
        var alternada: Estacion? = null
        selector(favoritos = listOf(estaciones[0]), onAlternarFavorito = { alternada = it })
        regla.onNodeWithContentDescription("Guardar San Miguelito en favoritos").performClick()
        assertEquals("C", alternada?.id)
        // Albrook es favorita: aparece arriba (Tus favoritos) y en la lista, ambas con "Quitar"
        regla.onAllNodesWithContentDescription("Quitar Albrook de favoritos").assertCountEquals(2)
    }

    @Test
    fun buscarFiltraYSePuedeBorrar() {
        selector()
        regla.onNodeWithText("Estación, dirección o lugar").performTextInput("carmen")
        regla.onNodeWithText("Iglesia del Carmen").assertExists()
        regla.onNodeWithText("Albrook").assertDoesNotExist()
        regla.onNodeWithContentDescription("Borrar búsqueda").performClick()
        regla.onNodeWithText("Albrook").assertExists()
    }

    @Test
    fun laEstacionYaElegidaSeMarca() {
        selector(seleccionado = estaciones[1].comoPunto())
        regla.onNode(hasText("Iglesia del Carmen") and hasStateDescription("elegida"), useUnmergedTree = false).assertExists()
    }
}

/** Plan de ejemplo con dos opciones: una más rápida con transbordo y otra directa. */
internal fun planDeEjemplo(aviso: String? = null, opciones: Boolean = true): PlanDto {
    val caminar = { min: Int -> TramoDto("CAMINAR", "A", "B", duracionMin = min, distanciaM = min * 80, instruccion = "Camina") }
    val l1 = TramoDto("METRO", "Albrook", "San Miguelito", linea = "1", colorLinea = "1C4F9C", duracionMin = 18, distanciaM = 9000, instruccion = "Toma la Línea 1")
    val l2 = TramoDto("METRO", "San Miguelito", "Corredor Sur", linea = "2", colorLinea = "2E9C47", duracionMin = 9, distanciaM = 5000, instruccion = "Toma la Línea 2")
    return PlanDto(
        opciones = if (!opciones) emptyList() else listOf(
            OpcionDto(32, 1, 640, listOf(caminar(4), l1, l2, caminar(4)), sale = "08:05", llega = "08:37"),
            OpcionDto(41, 0, 1200, listOf(caminar(9), l1, caminar(14)), sale = "08:02", llega = "08:43"),
        ),
        calidad = CalidadDto("verificado", "estimado", "estimado", "desconocido", "Los tiempos son estimados a partir de horarios publicados."),
        aviso = aviso,
    )
}
