package pa.conectapty.lugares;

import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import pa.conectapty.comun.ConfiguracionCache;

@Service
public class ServicioLugares {

    private final ClientePhoton photon;

    public ServicioLugares(ClientePhoton photon) {
        this.photon = photon;
    }

    /** Caché de 24 h: la misma búsqueda de muchos usuarios llega una sola vez a Photon. */
    @Cacheable(ConfiguracionCache.LUGARES)
    public List<Lugar> buscar(String texto, int limite) {
        return List.copyOf(photon.buscar(texto, limite));
    }
}
