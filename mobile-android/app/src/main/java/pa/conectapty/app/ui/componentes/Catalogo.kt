package pa.conectapty.app.ui.componentes

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pa.conectapty.app.R
import pa.conectapty.app.ui.tema.ConectaTema
import pa.conectapty.app.ui.tema.Espacio
import pa.conectapty.app.ui.tema.numerico

/**
 * Catálogo del sistema de diseño, solo para las vistas previas de Android Studio
 * (no se usa en la app; R8 lo elimina del APK de release).
 */
@Composable
internal fun Catalogo() {
    val cs = MaterialTheme.colorScheme
    Surface(color = cs.background) {
        Column(Modifier.padding(Espacio.l), verticalArrangement = Arrangement.spacedBy(Espacio.l)) {
            Box(Modifier.fillMaxWidth().background(ConectaTema.extra.encabezado, MaterialTheme.shapes.large)) {
                Image(
                    painterResource(R.drawable.skyline_panama), contentDescription = null,
                    colorFilter = ColorFilter.tint(ConectaTema.extra.onEncabezado.copy(alpha = 0.10f)),
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter),
                )
                Row(Modifier.padding(Espacio.l), verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(R.drawable.logo_c), contentDescription = null, modifier = Modifier.size(32.dp))
                    Column(Modifier.padding(start = Espacio.m)) {
                        Text("Conecta PTY", style = MaterialTheme.typography.titleLarge, color = ConectaTema.extra.onEncabezado)
                        Text("¿A dónde vas hoy?", style = MaterialTheme.typography.bodyMedium, color = ConectaTema.extra.onEncabezadoSecundario)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
                listOf(cs.primary, cs.secondary, ConectaTema.extra.acento, cs.tertiary, cs.error, ConectaTema.extra.exito, cs.outlineVariant)
                    .forEach { Box(Modifier.size(32.dp).background(it, MaterialTheme.shapes.small)) }
            }
            Column(verticalArrangement = Arrangement.spacedBy(Espacio.xs)) {
                Text("≈ 24 min", style = MaterialTheme.typography.headlineMedium.numerico)
                Text("Título de pantalla", style = MaterialTheme.typography.titleLarge)
                Text("Encabezado de sección", style = MaterialTheme.typography.titleSmall)
                Text("Texto normal para leer instrucciones del recorrido.", style = MaterialTheme.typography.bodyLarge)
                Text("Texto secundario", style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Espacio.m)) {
                listOf<ImageVector>(
                    IconosConecta.Metro, IconosConecta.Caminar, IconosConecta.Intercambiar, IconosConecta.Reloj,
                    IconosConecta.MiUbicacion, IconosConecta.Origen, IconosConecta.Bandera, IconosConecta.Mapa,
                    IconosConecta.SinConexion, IconosConecta.EstrellaVacia,
                ).forEach { Icon(it, contentDescription = null, tint = cs.primary) }
            }
            Tarjeta(onClick = {}) {
                Text("Tarjeta tocable", style = MaterialTheme.typography.titleMedium)
                Text("Borde fino, sin sombra", style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
            }
            Aviso("Tiempos estimados: pueden variar.", TipoAviso.ADVERTENCIA, detalle = "Costo del pasaje: no confirmado.")
            Aviso("Sin conexión: usando estaciones guardadas.", TipoAviso.ERROR, accion = "Reintentar")
            Aviso("Guardada en favoritos", TipoAviso.EXITO)
            Esqueleto(Modifier.fillMaxWidth())
            BotonSecundario("Ver mapa", onClick = {}, icono = IconosConecta.Mapa)
            BotonPrincipal("Buscar ruta", onClick = {})
            BotonPrincipal("Buscando…", onClick = {}, cargando = true)
            Box(Modifier.height(Espacio.s))
        }
    }
}

@Preview(name = "Sistema de diseño · claro", heightDp = 1100)
@Composable
private fun CatalogoClaro() = ConectaTema(oscuro = false) { Catalogo() }

@Preview(name = "Sistema de diseño · oscuro", heightDp = 1100, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CatalogoOscuro() = ConectaTema(oscuro = true) { Catalogo() }

@Preview(name = "Estado vacío")
@Composable
private fun EstadoVacioVista() = ConectaTema(oscuro = false) {
    Surface { EstadoVista(IconosConecta.SinConexion, "Sin conexión", "Revisa tu internet e inténtalo de nuevo.", accion = "Reintentar", esError = true) }
}
