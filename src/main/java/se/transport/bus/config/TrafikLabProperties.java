package se.transport.bus.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.validation.constraints.NotNull;

@Data
@ConfigurationProperties(prefix = "trafiklab")
public class TrafikLabProperties {

    @NotNull
    private String baseUrl;

    @NotNull
    private String apiKey;
}
