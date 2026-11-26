package pa.conectapty.reportes;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Lo que envía la app al tocar "Reportar un problema". Sin datos personales.
 *
 * @param paradaId estación relacionada (opcional): "EST_san-miguelito"
 * @param contexto qué estaba viendo el usuario (opcional): "Albrook → San Miguelito, 08:00"
 */
public record NuevoReporte(
        @NotNull Tipo tipo,
        @NotBlank @Size(min = 5, max = 1000) String mensaje,
        @Size(max = 64) String paradaId,
        @Size(max = 300) String contexto,
        @Size(max = 20) String versionApp) {

    public enum Tipo { ESTACION, RUTA, SUGERENCIA, OTRO }
}
