package pa.conectapty.lugares;

/**
 * Dirección o lugar encontrado por el geocodificador ("UTP", "Albrook Mall", "Calle 50").
 *
 * @param detalle zona para distinguir homónimos: "Bella Vista, Panamá"
 * @param tipo    categoría de OpenStreetMap ("university", "mall", "street"...), útil para el ícono
 */
public record Lugar(String nombre, String detalle, double lat, double lon, String tipo) {}
