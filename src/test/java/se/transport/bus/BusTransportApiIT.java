package se.transport.bus;

import se.transport.bus.api.model.BusLine;
import se.transport.bus.config.WireMockInitializer;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.hazelcast.core.HazelcastInstance;
import org.junit.After;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.request;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = {BusTransportApplication.class})
@ContextConfiguration(initializers = WireMockInitializer.class)
@ActiveProfiles({"local", "test"})
public class BusTransportApiIT {

    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    private WebTestClient webTestClient;

    @Autowired
    private HazelcastInstance hazelcastInstance;

    @After
    public void cleanUp() {
        hazelcastInstance.getMap("bus-stops").clear();
        hazelcastInstance.getMap("journey-points").clear();
    }

    @Test
    public void givenLimit_whenGetTopMostBusLines_thenSuccessfulResponse() throws InterruptedException {
        mockBackendWithFile("GET", "/", 200, "jour", MediaType.APPLICATION_JSON_VALUE, "JourneyPoint.json");

        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        queryParams.add("query-limit", "10");

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/bus/lines/max-stops").queryParams(queryParams).build())
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(BusLine.class).hasSize(10);

        Thread.sleep(3000);

        Assertions.assertTrue(hazelcastInstance.getMap("journey-points").isEmpty());
    }

    @Test
    public void givenRequestWithoutLimit_whenGetTopMostBusLines_thenErrorResponse() {
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/bus/lines/max-stops").build())
                .exchange()
                .expectStatus().isBadRequest().expectBody().jsonPath("$.error").isNotEmpty();
    }

    @Test
    public void givenRequest_backendHttpError_whenGetTopMostBusLines_thenErrorResponse() {
        mockBackendWithFile("GET", "/", 400, "jour", MediaType.APPLICATION_JSON_VALUE, "ErrorStatus.json");

        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        queryParams.add("query-limit", "10");

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/bus/lines/max-stops").queryParams(queryParams).build())
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    public void givenRequest_backendErrorStatus_whenGetTopMostBusLines_thenErrorResponse() {
        mockBackendWithFile("GET", "/", 200, "jour", MediaType.APPLICATION_JSON_VALUE, "ErrorStatus.json");

        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        queryParams.add("query-limit", "10");

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/bus/lines/max-stops").queryParams(queryParams).build())
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    public void givenRequestWithLineNumber_whenGetBusStops_thenSuccessfulResponse() throws InterruptedException {
        mockBackendWithFile("GET", "/", 200, "jour", MediaType.APPLICATION_JSON_VALUE, "JourneyPoint.json");
        mockBackendWithFile("GET", "/", 200, "stop", MediaType.APPLICATION_JSON_VALUE, "Stop.json");

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/bus/lines/636/stops").build())
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.lineNumber").isNotEmpty()
                .jsonPath("$.busStops").isNotEmpty();


        Thread.sleep(3000);

        Assertions.assertTrue(hazelcastInstance.getMap("bus-stops").isEmpty());
    }

    @Test
    public void givenRequestWithNonExistingLineNumber_whenGetBusStops_thenErrorResponse() {
        mockBackendWithFile("GET", "/", 200, "jour", MediaType.APPLICATION_JSON_VALUE, "JourneyPoint.json");
        mockBackendWithFile("GET", "/", 200, "stop", MediaType.APPLICATION_JSON_VALUE, "Stop.json");

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/bus/lines/0/stops").build())
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.error").isEqualTo("Invalid Line Number");
    }

    @Test
    public void givenRequest_backendHttpErrorInJourneyPointsResponse_whenGetBusStops_thenErrorResponse() {
        mockBackendWithFile("GET", "/", 400, "jour", MediaType.APPLICATION_JSON_VALUE, "ErrorStatus.json");

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/bus/lines/636/stops").build())
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody().jsonPath("$.error").isNotEmpty();
    }

    @Test
    public void givenRequest_backendHttpErrorInStopPointResponse_whenGetBusStops_thenErrorResponse() {
        mockBackendWithFile("GET", "/", 200, "jour", MediaType.APPLICATION_JSON_VALUE, "JourneyPoint.json");
        mockBackendWithFile("GET", "/", 400, "stop", MediaType.APPLICATION_JSON_VALUE, "ErrorStatus.json");

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/bus/lines/636/stops").build())
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody().jsonPath("$.error").isNotEmpty();
    }

    @Test
    public void givenRequest_backendErrorStatus_whenGetBusStops_thenErrorResponse() {
        mockBackendWithFile("GET", "/", 200, "jour", MediaType.APPLICATION_JSON_VALUE, "ErrorStatus.json");

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/bus/lines/636/stops").build())
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody().jsonPath("$.error").isNotEmpty();
    }

    public static void mockBackendWithFile(String method, String uri, int status, String queryValue, String contentType, String responseBodyFile) {
        stubFor(request(method, urlPathMatching(uri))
                .withQueryParam("model", new EqualToPattern(queryValue))
                .willReturn(aResponse()
                        .withHeader("Content-Type", contentType)
                        .withStatus(status)
                        .withBodyFile(responseBodyFile)));
    }

}
