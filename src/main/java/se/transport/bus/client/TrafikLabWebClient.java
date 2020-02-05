package se.transport.bus.client;

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
import se.transport.bus.client.response.BaseResponse;
import se.transport.bus.client.response.JourneyPatternPointResponse;
import se.transport.bus.client.response.StopPoint;
import se.transport.bus.config.TrafikLabProperties;
import se.transport.bus.exception.HttpIntegrationException;
import se.transport.bus.service.HazelcastCacheService;

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
                callTrafikLabApi(queryMap, "journey pattern", new ParameterizedTypeReference<BaseResponse<JourneyPatternPointResponse>>() {
                }, journeyCircuitBreakerOperator)
                        .doOnError(throwable -> log.error("error from backend getJourneyPatternPoints", throwable))
        );
    }

    @SuppressWarnings("unchecked")
    public Mono<BaseResponse<StopPoint>> getStopPoints() {
        MultiValueMap<String, String> queryMap = new LinkedMultiValueMap<>();
        queryMap.add(MODEL, STOP_KEY);
        return cacheService.findFromCache("bus-stops", STOP_KEY,
                callTrafikLabApi(queryMap, "stop points", new ParameterizedTypeReference<BaseResponse<StopPoint>>() {
                }, stopCircuitBreakerOperator)
                        .doOnError(throwable -> log.error("error from backend getStopPoints", throwable))
        );
    }

    private <T> Mono<BaseResponse<T>> callTrafikLabApi(MultiValueMap<String, String> queryMap,
                                                       String operation, ParameterizedTypeReference<BaseResponse<T>> typeReference,
                                                       CircuitBreakerOperator<BaseResponse<T>> circuitBreakerOperator) {
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
                }))
                .bodyToMono(typeReference)
                .transform(circuitBreakerOperator)
                .doOnSuccess(response -> validateResponse(response.getStatusCode()));
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
