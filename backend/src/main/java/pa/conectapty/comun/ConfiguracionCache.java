package pa.conectapty.comun;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Cada caché con su propia vida: una ruta cambia en minutos, una dirección casi nunca. */
@Configuration
public class ConfiguracionCache {

    public static final String PLANES = "planes";
    public static final String LUGARES = "lugares";

    @Bean
    CacheManager cacheManager() {
        CaffeineCacheManager m = new CaffeineCacheManager();
        // 60 s: búsquedas casi iguales no repiten el cálculo en OTP, pero la hora "ahora" no se queda vieja
        m.registerCustomCache(PLANES, Caffeine.newBuilder()
                .maximumSize(2_000).expireAfterWrite(Duration.ofSeconds(60)).build());
        // 24 h: cuida el uso justo del geocodificador público y responde al instante
        m.registerCustomCache(LUGARES, Caffeine.newBuilder()
                .maximumSize(5_000).expireAfterWrite(Duration.ofHours(24)).build());
        m.setAllowNullValues(false);
        return m;
    }
}
