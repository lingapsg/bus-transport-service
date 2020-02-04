package se.transport.bus.api;

import se.transport.bus.api.model.BusLine;
import se.transport.bus.api.model.BusStops;
import se.transport.bus.error.ApiError;
import se.transport.bus.exception.BusTransportException;
import se.transport.bus.exception.HttpIntegrationException;
import se.transport.bus.service.BusTransportService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;


@Validated
@Slf4j
@Api(tags = "Bus Transport")
@RequiredArgsConstructor
@RestController
public class BusTransportApi {

    private final BusTransportService busTransportService;

    @ApiOperation(value = "find top most bus lines", notes = "This endpoint returns top most bus lines fot the given query limit")
    @ApiResponses({
            @ApiResponse(code = HTTP_BAD_REQUEST, message = "Invalid request", response = ApiError.class)
    })
    @GetMapping(value = "/bus/lines/max-stops", produces = MediaType.APPLICATION_JSON_VALUE)
    public Flux<BusLine> getTopMostBusLines(@RequestParam("query-limit") @NotNull(message = "query-limit cannot be null") @Min(1) Long queryLimit) {
        return busTransportService.findTopMostBusLines(queryLimit);
    }

    @ApiOperation(value = "find bus stops", notes = "This endpoint returns bus stops for the given lineNumber")
    @ApiResponses({
            @ApiResponse(code = HTTP_BAD_REQUEST, message = "Invalid request", response = ApiError.class)
    })
    @GetMapping(value = "/bus/lines/{line-number}/stops", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<BusStops> getBusStopsForLine(@PathVariable("line-number") String lineNumber) {
        return busTransportService.getBusStopsForLine(lineNumber);
    }

    @ExceptionHandler(BusTransportException.class)
    public ResponseEntity<ApiError> handleBusTransportException(BusTransportException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiError.builder().error(e.getMessage()).build());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleRequestParamException(MissingServletRequestParameterException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiError.builder().error(e.getMessage()).build());
    }

    @ExceptionHandler(HttpIntegrationException.class)
    public ResponseEntity<ApiError> handleIntegrationException(HttpIntegrationException e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiError.builder().error(e.getMessage()).build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> unhandledException(Exception e) {
        log.error("unhandled Exception occurred", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiError.builder().error(e.getMessage()).build());
    }
}
