package pa.conectapty.planificacion;

import java.util.ArrayList;
import java.util.List;
import pa.conectapty.planificacion.Itinerario.Opcion;
import pa.conectapty.planificacion.Itinerario.Tramo;
import pa.conectapty.planificacion.OtpRespuesta.ItinerarioOtp;
import pa.conectapty.planificacion.OtpRespuesta.Leg;

/** Convierte la respuesta técnica de OTP en instrucciones en lenguaje sencillo (sección 9.2). */
public final class TraductorItinerario {

    private TraductorItinerario() {}

    public static List<Opcion> traducir(OtpRespuesta r) {
        if (r.data() == null || r.data().plan() == null || r.data().plan().itineraries() == null) {
            return List.of();
        }
        List<Opcion> opciones = new ArrayList<>();
        for (ItinerarioOtp it : r.data().plan().itineraries()) {
            List<Tramo> tramos = new ArrayList<>();
            int enTransporte = 0;
            for (Leg leg : it.legs()) {
                boolean caminar = "WALK".equals(leg.mode());
                if (!caminar) enTransporte++;
                // Tramos de caminata de menos de 20 m son ruido (cambio de andén en la misma estación)
                if (caminar && leg.distance() < 20) continue;
                tramos.add(tramo(leg, caminar));
            }
            opciones.add(new Opcion(
                    minutos(it.duration()),
                    Math.max(0, enTransporte - 1),
                    (int) Math.round(it.walkDistance()),
                    List.copyOf(tramos)));
        }
        return opciones;
    }

    static Tramo tramo(Leg leg, boolean caminar) {
        String desde = nombre(leg.from());
        String hasta = nombre(leg.to());
        int distancia = (int) Math.round(leg.distance());
        int duracion = minutos(leg.duration());
        String geometria = leg.legGeometry() == null ? null : leg.legGeometry().points();
        if (caminar) {
            String destino = "Destino".equalsIgnoreCase(hasta) ? "tu destino" : hasta;
            return new Tramo("CAMINAR", desde, hasta, null, null, null, 0, duracion, distancia,
                    "Camina " + metrosLegibles(distancia) + " hasta " + destino + ".", geometria);
        }
        String linea = leg.route() == null ? "?" : leg.route().shortName();
        String color = leg.route() == null ? null : leg.route().color();
        int paradas = (leg.intermediateStops() == null ? 0 : leg.intermediateStops().size()) + 1;
        String direccion = leg.headsign();
        String instruccion = "En " + desde + ", toma la Línea " + linea
                + (direccion != null && !direccion.isBlank() ? " hacia " + direccion : "")
                + " y bájate en " + hasta + " (" + paradas + (paradas == 1 ? " parada" : " paradas") + ").";
        return new Tramo("METRO", desde, hasta, linea, color, direccion, paradas, duracion, distancia,
                instruccion, geometria);
    }

    private static String nombre(OtpRespuesta.Lugar l) {
        if (l == null || l.name() == null) return "?";
        return switch (l.name()) {
            case "Origin" -> "Origen";
            case "Destination" -> "Destino";
            default -> l.name();
        };
    }

    static int minutos(double segundos) {
        return (int) Math.max(1, Math.round(segundos / 60.0));
    }

    static String metrosLegibles(int m) {
        if (m < 1000) return (Math.round(m / 10.0) * 10) + " m";
        return String.format(java.util.Locale.ROOT, "%.1f km", m / 1000.0);
    }
}
