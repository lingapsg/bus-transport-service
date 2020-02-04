package se.transport.bus.client;

import se.transport.bus.client.response.BaseResponse;
import se.transport.bus.client.response.JourneyPatternPointResponse;
import se.transport.bus.client.response.StopPoint;
import se.transport.bus.config.TrafikLabProperties;
import se.transport.bus.exception.HttpIntegrationException;
import se.transport.bus.service.HazelcastCacheService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Slf4j
@Component
public class TrafikLabWebClient {

    private final WebClient backendWebClient;
    private final HazelcastCacheService cacheService;
    private final TrafikLabProperties trafikLabProperties;
    private final CircuitBreakerOperator<BaseResponse<JourneyPatternPointResponse>> journeyCircuitBreakerOperator;
    private final CircuitBreakerOperator<BaseResponse<StopPoint>> stopCircuitBreakerOperator;

    public TrafikLabWebClient(@Qualifier("backendClient") WebClient backendWebClient,
                              HazelcastCacheService cacheService,
                              TrafikLabProperties trafikLabProperties,
                              CircuitBreaker circuitBreaker) {
        this.backendWebClient = backendWebClient;
        this.cacheService = cacheService;
        this.trafikLabProperties = trafikLabProperties;
        this.journeyCircuitBreakerOperator = CircuitBreakerOperator.of(circuitBreaker);
        this.stopCircuitBreakerOperator = CircuitBreakerOperator.of(circuitBreaker);
    }

    private static final String MODEL = "model";
    private static final String JOURNEY_KEY = "jour";
    private static final String STOP_KEY = "stop";

    @SuppressWarnings("unchecked")
    public Mono<BaseResponse<JourneyPatternPointResponse>> getJourneyPatternPoints() {
        MultiValueMap<String, String> queryMap = new LinkedMultiValueMap<>();
        queryMap.add(MODEL, JOURNEY_KEY);
        return cacheService.findFromCache("journey-points", JOURNEY_KEY,
                callTrafikLabApi(queryMap, "journey pattern")
                        .bodyToMono(new ParameterizedTypeReference<BaseResponse<JourneyPatternPointResponse>>() {
                        })
                        .transform(journeyCircuitBreakerOperator)
                        .doOnError(throwable -> log.error("error from backend getJourneyPatternPoints", throwable))
                        .doOnSuccess(journeyPatternPointResponse -> validateResponse(journeyPatternPointResponse.getStatusCode()))
        );
    }

    @SuppressWarnings("unchecked")
    public Mono<BaseResponse<StopPoint>> getStopPoints() {
        MultiValueMap<String, String> queryMap = new LinkedMultiValueMap<>();
        queryMap.add(MODEL, STOP_KEY);
        return cacheService.findFromCache("bus-stops", STOP_KEY,
                callTrafikLabApi(queryMap, "stop points")
                        .bodyToMono(new ParameterizedTypeReference<BaseResponse<StopPoint>>() {
                        })
                        .transform(stopCircuitBreakerOperator)
                        .doOnError(throwable -> log.error("error from backend getStopPoints", throwable))
                        .doOnSuccess(stopPoints -> validateResponse(stopPoints.getStatusCode()))
        );
    }

    private WebClient.ResponseSpec callTrafikLabApi(MultiValueMap<String, String> queryMap, String operation) {
        queryMap.add("key", trafikLabProperties.getApiKey());
        queryMap.add("DefaultTransportModeCode", "BUS");
        return backendWebClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .queryParams(queryMap)
                        .build())
                .retrieve()
                .onStatus(HttpStatus::isError, clientResponse -> clientResponse.bodyToMono(String.class).flatMap(error -> {
                    log.error("Error while getting {} : {}", operation, error);
                    return Mono.error(new HttpIntegrationException(error));
                }));
    }

    private void validateResponse(Integer statusCode) {
        Optional.ofNullable(statusCode)
                .ifPresent(status -> {
                    if (status != 0) {
                        log.error("Received error response {}", status);
                        throw new HttpIntegrationException("something went wrong");
                    }
                });
    }
}
