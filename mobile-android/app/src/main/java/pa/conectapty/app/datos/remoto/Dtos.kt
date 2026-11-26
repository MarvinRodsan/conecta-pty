package pa.conectapty.app.datos.remoto

import kotlinx.serialization.Serializable

// Espejo exacto de la API /api/v1 del backend. Inmutables -> Compose los trata como estables.

@Serializable
data class ParadaDto(
    val id: String,
    val nombre: String,
    val lat: Double,
    val lon: Double,
    val distanciaM: Int? = null,
    val accesible: String = "desconocido",
)

@Serializable
data class PlanDto(
    val opciones: List<OpcionDto>,
    val calidad: CalidadDto,
    val salidaConsultada: String? = null,
    /** Mensaje importante para el usuario, p. ej. "el Metro no opera a esta hora". */
    val aviso: String? = null,
)

@Serializable
data class OpcionDto(
    val duracionMin: Int,
    val transbordos: Int,
    val caminataM: Int,
    val tramos: List<TramoDto>,
    val sale: String? = null,
    val llega: String? = null,
    val usaMetro: Boolean = true,
)

@Serializable
data class TramoDto(
    val modo: String,
    val desde: String,
    val hasta: String,
    val linea: String? = null,
    val colorLinea: String? = null,
    val direccion: String? = null,
    val paradas: Int = 0,
    val duracionMin: Int,
    val distanciaM: Int,
    val sale: String? = null,
    val llega: String? = null,
    val instruccion: String,
    val geometria: String? = null,
)

@Serializable
data class CalidadDto(
    val estaciones: String,
    val tiempos: String,
    val frecuencias: String,
    val tarifas: String,
    val aviso: String,
)

/** Resultado de /api/v1/lugares: dirección o lugar ("UTP", "Albrook Mall"). */
@Serializable
data class LugarDto(
    val nombre: String,
    val detalle: String? = null,
    val lat: Double,
    val lon: Double,
    val tipo: String? = null,
)

@Serializable
data class SaludDto(
    val estado: String,
    val version: String? = null,
    val datos: DatosDto? = null,
)

/** Versión de datos que sirve el backend, para "Datos actualizados el …". */
@Serializable
data class DatosDto(
    val actualizadoEl: String,
    val fuente: String,
    val licencia: String? = null,
)

/** Cuerpo de POST /api/v1/reportes. Sin datos personales ni ubicación. */
@Serializable
data class NuevoReporteDto(
    val tipo: String,
    val mensaje: String,
    val paradaId: String? = null,
    val contexto: String? = null,
    val versionApp: String? = null,
)

@Serializable
data class ReporteRecibidoDto(val id: Long, val mensaje: String)
