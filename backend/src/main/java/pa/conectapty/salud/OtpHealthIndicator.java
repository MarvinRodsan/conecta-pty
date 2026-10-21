package pa.conectapty.salud;

import java.time.Duration;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import pa.conectapty.comun.ConectaProperties;

/** Aparece en /actuator/health como "otp": UP o DOWN. */
@Component("otp")
public class OtpHealthIndicator implements HealthIndicator {

    private final RestClient http;
    private final String url;

    public OtpHealthIndicator(RestClient.Builder builder, ConectaProperties props) {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(Duration.ofSeconds(2));
        f.setReadTimeout(Duration.ofSeconds(2));
        this.url = props.otp().url();
        this.http = builder.baseUrl(url).requestFactory(f).build();
    }

    @Override
    public Health health() {
        try {
            http.get().uri("/otp/").retrieve().toBodilessEntity();
            return Health.up().withDetail("url", url).build();
        } catch (Exception e) {
            return Health.down().withDetail("url", url).withDetail("error", e.getMessage()).build();
        }
    }
}
