package pa.conectapty.paradas;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class ParadaRepositorio {

    private final JdbcClient jdbc;

    public ParadaRepositorio(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Estaciones dentro de un radio, de la más cercana a la más lejana. Usa el índice GIST. */
    public List<Parada> cercanas(double lat, double lon, int radioM, int limite) {
        return jdbc.sql("""
                SELECT id, nombre, ST_Y(ubicacion::geometry) AS lat, ST_X(ubicacion::geometry) AS lon,
                       ROUND(ST_Distance(ubicacion, ST_MakePoint(:lon, :lat)::geography))::int AS distancia_m,
                       accesible
                FROM parada
                WHERE tipo = 1
                  AND ST_DWithin(ubicacion, ST_MakePoint(:lon, :lat)::geography, :radio)
                ORDER BY ubicacion <-> ST_MakePoint(:lon, :lat)::geography
                LIMIT :limite
                """)
                .param("lat", lat).param("lon", lon).param("radio", radioM).param("limite", limite)
                .query(ParadaRepositorio::mapear)
                .list();
    }

    /** Búsqueda por nombre sin importar tildes ni mayúsculas ("via argentina" encuentra "Vía Argentina"). */
    public List<Parada> buscar(String texto, int limite) {
        return jdbc.sql("""
                SELECT id, nombre, ST_Y(ubicacion::geometry) AS lat, ST_X(ubicacion::geometry) AS lon,
                       NULL::int AS distancia_m, accesible
                FROM parada
                WHERE tipo = 1 AND unaccent(lower(nombre)) LIKE '%' || unaccent(lower(:q)) || '%'
                ORDER BY nombre
                LIMIT :limite
                """)
                .param("q", texto).param("limite", limite)
                .query(ParadaRepositorio::mapear)
                .list();
    }

    public List<Parada> todas() {
        return jdbc.sql("""
                SELECT id, nombre, ST_Y(ubicacion::geometry) AS lat, ST_X(ubicacion::geometry) AS lon,
                       NULL::int AS distancia_m, accesible
                FROM parada WHERE tipo = 1 ORDER BY nombre
                """)
                .query(ParadaRepositorio::mapear)
                .list();
    }

    private static Parada mapear(ResultSet rs, int fila) throws SQLException {
        int d = rs.getInt("distancia_m");
        Integer distancia = rs.wasNull() ? null : d;
        String accesible = switch (rs.getShort("accesible")) {
            case 1 -> "si";
            case 2 -> "no";
            default -> "desconocido";
        };
        return new Parada(rs.getString("id"), rs.getString("nombre"),
                rs.getDouble("lat"), rs.getDouble("lon"), distancia, accesible);
    }
}
