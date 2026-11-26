package pa.conectapty.app.ui.componentes

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Íconos que material-icons-core no trae. Trazos de Material Icons (Apache 2.0), 24 × 24.
 * Se dibujan aquí en vez de sumar material-icons-extended (varios MB) por una decena de íconos.
 */
object IconosConecta {
    val Intercambiar by lazy { icono("Intercambiar", "M16,17.01V10h-2v7.01h-3L15,21l4,-3.99h-3zM9,3L5,6.99h3V14h2V6.99h3L9,3z") }
    val Caminar by lazy {
        icono(
            "Caminar",
            "M13.5,5.5c1.1,0 2,-0.9 2,-2s-0.9,-2 -2,-2 -2,0.9 -2,2 0.9,2 2,2zM9.8,8.9L7,23h2.1l1.8,-8 2.1,2v6h2v-7.5l-2.1,-2 0.6,-3C14.8,12 16.8,13 19,13v-2c-1.9,0 -3.5,-1 -4.3,-2.4l-1,-1.6c-0.4,-0.6 -1,-1 -1.7,-1 -0.3,0 -0.5,0.1 -0.8,0.1L6,8.3V13h2V9.6l1.8,-0.7",
        )
    }
    val Metro by lazy {
        icono(
            "Metro",
            "M12,2c-4.42,0 -8,0.5 -8,4v10.5C4,18.43 5.57,20 7.5,20L6,21.5v0.5h12v-0.5L16.5,20c1.93,0 3.5,-1.57 3.5,-3.5V6c0,-3.5 -3.58,-4 -8,-4zM7.5,17c-0.83,0 -1.5,-0.67 -1.5,-1.5S6.67,14 7.5,14s1.5,0.67 1.5,1.5S8.33,17 7.5,17zM11,11H6V6h5v5zM16.5,17c-0.83,0 -1.5,-0.67 -1.5,-1.5s0.67,-1.5 1.5,-1.5 1.5,0.67 1.5,1.5 -0.67,1.5 -1.5,1.5zM18,11h-5V6h5v5z",
        )
    }
    val Reloj by lazy {
        icono(
            "Reloj",
            "M11.99,2C6.47,2 2,6.48 2,12s4.47,10 9.99,10C17.52,22 22,17.52 22,12S17.52,2 11.99,2zM12,20c-4.42,0 -8,-3.58 -8,-8s3.58,-8 8,-8 8,3.58 8,8 -3.58,8 -8,8zM12.5,7H11v6l5.25,3.15 0.75,-1.23 -4.5,-2.67z",
        )
    }
    val MiUbicacion by lazy {
        icono(
            "MiUbicacion",
            "M12,8c-2.21,0 -4,1.79 -4,4s1.79,4 4,4 4,-1.79 4,-4 -1.79,-4 -4,-4zM20.94,11c-0.46,-4.17 -3.77,-7.48 -7.94,-7.94V1h-2v2.06C6.83,3.52 3.52,6.83 3.06,11H1v2h2.06c0.46,4.17 3.77,7.48 7.94,7.94V23h2v-2.06c4.17,-0.46 7.48,-3.77 7.94,-7.94H23v-2h-2.06zM12,19c-3.87,0 -7,-3.13 -7,-7s3.13,-7 7,-7 7,3.13 7,7 -3.13,7 -7,7z",
        )
    }
    val SinConexion by lazy {
        icono(
            "SinConexion",
            "M19.35,10.04C18.67,6.59 15.64,4 12,4c-1.48,0 -2.85,0.43 -4.01,1.17l1.46,1.46C10.21,6.23 11.08,6 12,6c3.04,0 5.5,2.46 5.5,5.5v0.5H19c1.66,0 3,1.34 3,3 0,1.13 -0.64,2.11 -1.56,2.62l1.45,1.45C23.16,18.16 24,16.68 24,15c0,-2.64 -2.05,-4.78 -4.65,-4.96zM3,5.27l2.75,2.74C2.56,8.15 0,10.77 0,14c0,3.31 2.69,6 6,6h11.73l2,2L21,20.73 4.27,4 3,5.27zM7.73,10l8,8H6c-2.21,0 -4,-1.79 -4,-4s1.79,-4 4,-4h1.73z",
        )
    }
    val ErrorCirculo by lazy {
        icono(
            "ErrorCirculo",
            "M11,15h2v2h-2zM11,7h2v6h-2zM11.99,2C6.47,2 2,6.48 2,12s4.47,10 9.99,10C17.52,22 22,17.52 22,12S17.52,2 11.99,2zM12,20c-4.42,0 -8,-3.58 -8,-8s3.58,-8 8,-8 8,3.58 8,8 -3.58,8 -8,8z",
        )
    }
    val Mapa by lazy {
        icono(
            "Mapa",
            "M20.5,3l-0.16,0.03L15,5.1 9,3 3.36,4.9c-0.21,0.07 -0.36,0.25 -0.36,0.48V20.5c0,0.28 0.22,0.5 0.5,0.5l0.16,-0.03L9,18.9l6,2.1 5.64,-1.9c0.21,-0.07 0.36,-0.25 0.36,-0.48V3.5c0,-0.28 -0.22,-0.5 -0.5,-0.5zM15,19l-6,-2.11V5l6,2.11V19z",
        )
    }
    val Bandera by lazy { icono("Bandera", "M14.4,6L14,4H5v17h2v-7h5.6l0.4,2h7V6z") }
    /** Punto de origen (anillo): se distingue del destino por la forma, no solo por el color. */
    val Origen by lazy {
        icono("Origen", "M2,12C2,6.48 6.48,2 12,2s10,4.48 10,10 -4.48,10 -10,10S2,17.52 2,12zM12,18c3.31,0 6,-2.69 6,-6s-2.69,-6 -6,-6 -6,2.69 -6,6 2.69,6 6,6z")
    }
    /** Estrella vacía ("star_border"): Icons.Outlined.Star de core es rellena. */
    val EstrellaVacia by lazy {
        icono(
            "EstrellaVacia",
            "M22,9.24l-7.19,-0.62L12,2 9.19,8.63 2,9.24l5.46,4.73L5.82,21 12,17.27 18.18,21l-1.63,-7.03L22,9.24zM12,15.4l-3.76,2.27 1,-4.28 -3.32,-2.88 4.38,-0.38L12,6.1l1.71,4.04 4.38,0.38 -3.32,2.88 1,4.28L12,15.4z",
        )
    }
}

private fun icono(nombre: String, trazo: String): ImageVector =
    ImageVector.Builder(nombre, 24.dp, 24.dp, 24f, 24f)
        .addPath(addPathNodes(trazo), fill = SolidColor(Color.Black))
        .build()
