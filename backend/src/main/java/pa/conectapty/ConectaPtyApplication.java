package pa.conectapty;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableCaching
public class ConectaPtyApplication {
    public static void main(String[] args) {
        SpringApplication.run(ConectaPtyApplication.class, args);
    }
}
