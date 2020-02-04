package se.transport.bus.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.SSLException;

@Slf4j
@Configuration
@EnableConfigurationProperties(TrafikLabProperties.class)
public class WebClientConfiguration {

    @Bean
    @Qualifier("backendClient")
    public WebClient backendWebClientBuilder(TrafikLabProperties trafikLabProperties) throws SSLException {

        HttpClient httpClient = HttpClient.create()
                .wiretap(true)
                .compress(true);

        return WebClient.builder()
                .baseUrl(trafikLabProperties.getBaseUrl())
                .defaultHeaders(httpHeaders -> httpHeaders.add("Accept-Encoding", "gzip, deflate"))
                .exchangeStrategies(ExchangeStrategies.
                        builder()
                        .codecs(clientCodecConfigurer -> clientCodecConfigurer.defaultCodecs().maxInMemorySize(-1))
                        .build())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    @Bean
    public CircuitBreaker circuitBreaker(TrafikLabProperties trafikLabProperties) {
        return CircuitBreaker.ofDefaults(trafikLabProperties.getBaseUrl());
    }
}
