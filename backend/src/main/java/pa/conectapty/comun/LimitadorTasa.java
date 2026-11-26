package pa.conectapty.comun;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * Límite de peticiones por clave (IP) con ventana fija de 1 minuto.
 * Sin dependencias: memoria acotada (se limpian ventanas viejas) y seguro con muchos hilos.
 *
 * Es deliberadamente simple. Para varios servidores detrás de un balanceador, el límite
 * debe moverse al proxy (Caddy/Nginx/Cloudflare) o a un almacén compartido.
 */
public final class LimitadorTasa {

    private static final long MINUTO_MS = 60_000;
    private static final int LIMPIAR_DESDE = 10_000;     // claves en memoria antes de limpiar

    private final int maximoPorMinuto;
    private final LongSupplier reloj;
    private final Map<String, Ventana> ventanas = new ConcurrentHashMap<>();

    public LimitadorTasa(int maximoPorMinuto, LongSupplier reloj) {
        if (maximoPorMinuto <= 0) throw new IllegalArgumentException("maximoPorMinuto debe ser > 0");
        this.maximoPorMinuto = maximoPorMinuto;
        this.reloj = reloj;
    }

    public LimitadorTasa(int maximoPorMinuto) {
        this(maximoPorMinuto, System::currentTimeMillis);
    }

    /** @return segundos que hay que esperar, o 0 si la petición se permite */
    public long intentar(String clave) {
        long ahora = reloj.getAsLong();
        long minuto = ahora / MINUTO_MS;
        if (ventanas.size() > LIMPIAR_DESDE) limpiar(minuto);
        Ventana v = ventanas.compute(clave, (k, actual) ->
                actual == null || actual.minuto != minuto ? new Ventana(minuto, 1) : actual.sumar());
        if (v.cuenta <= maximoPorMinuto) return 0;
        long finVentana = (minuto + 1) * MINUTO_MS;
        return Math.max(1, (finVentana - ahora + 999) / 1000);
    }

    int clavesEnMemoria() {
        return ventanas.size();
    }

    private void limpiar(long minutoActual) {
        for (Iterator<Ventana> it = ventanas.values().iterator(); it.hasNext(); ) {
            if (it.next().minuto < minutoActual) it.remove();
        }
    }

    private record Ventana(long minuto, int cuenta) {
        Ventana sumar() {
            return new Ventana(minuto, cuenta + 1);
        }
    }
}
