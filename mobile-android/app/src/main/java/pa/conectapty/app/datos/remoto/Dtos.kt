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
data class PlanDto(val opciones: List<OpcionDto>, val calidad: CalidadDto)

@Serializable
data class OpcionDto(
    val duracionMin: Int,
    val transbordos: Int,
    val caminataM: Int,
    val tramos: List<TramoDto>,
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
