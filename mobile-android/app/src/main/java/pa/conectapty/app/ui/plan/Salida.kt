package pa.conectapty.app.ui.plan

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Cuándo quiere salir el usuario. "En 30 min" se calcula al buscar, no al tocar el botón. */
sealed interface Salida {
    data object Ahora : Salida
    data object En30Min : Salida
    data class A(val cuando: LocalDateTime) : Salida
}

/** El backend y el Metro usan la hora de Panamá, aunque el teléfono tenga otra zona horaria. */
val ZONA_PANAMA: ZoneId = ZoneId.of("America/Panama")
val ESPANOL: Locale = Locale.forLanguageTag("es-PA")

fun ahoraEnPanama(): LocalDateTime = LocalDateTime.now(ZONA_PANAMA)

private val PARAMETRO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
private val HORA = DateTimeFormatter.ofPattern("HH:mm")
private val DIA = DateTimeFormatter.ofPattern("EEE d MMM", ESPANOL)

/** Valor de `salida=` para /api/v1/plan; null = "ahora" (lo decide el servidor). */
fun Salida.comoParametro(ahora: LocalDateTime): String? = when (this) {
    Salida.Ahora -> null
    Salida.En30Min -> ahora.plusMinutes(30).format(PARAMETRO)
    is Salida.A -> cuando.format(PARAMETRO)
}

/** "Salir ahora", "Salir en 30 min", "Salir hoy 08:00", "Salir mañana 07:30", "Salir mié 5 nov 08:00". */
fun Salida.describir(ahora: LocalDateTime): String = when (this) {
    Salida.Ahora -> "Salir ahora"
    Salida.En30Min -> "Salir en 30 min"
    is Salida.A -> "Salir ${describirMomento(cuando, ahora.toLocalDate())}"
}

fun describirMomento(cuando: LocalDateTime, hoy: LocalDate): String {
    val dia = when (cuando.toLocalDate()) {
        hoy -> "hoy"
        hoy.plusDays(1) -> "mañana"
        else -> cuando.format(DIA).replace(".", "")
    }
    return "$dia ${cuando.format(HORA)}"
}

/** "2026-10-28" -> "28 oct 2026". Si no se entiende, se muestra tal cual (nunca inventar una fecha). */
fun fechaLegible(iso: String): String =
    runCatching { LocalDate.parse(iso.take(10)).format(DateTimeFormatter.ofPattern("d MMM yyyy", ESPANOL)).replace(".", "") }
        .getOrDefault(iso)

/** 340 -> "≈ 340 m", 1250 -> "≈ 1,3 km". */
fun distanciaLegible(metros: Int): String =
    if (metros < 1000) "≈ ${(metros + 5) / 10 * 10} m"
    else "≈ ${String.format(ESPANOL, "%.1f", metros / 1000.0)} km"
