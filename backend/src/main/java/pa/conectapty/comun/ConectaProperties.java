package pa.conectapty.comun;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "conecta")
public record ConectaProperties(Otp otp, Gtfs gtfs) {
    public record Otp(String url, int timeoutMs) {}
    public record Gtfs(String path) {}
}
