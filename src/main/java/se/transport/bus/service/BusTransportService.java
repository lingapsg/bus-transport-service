package se.transport.bus.service;

import se.transport.bus.api.model.BusLine;
import se.transport.bus.api.model.BusStops;
import se.transport.bus.client.TrafikLabWebClient;
import se.transport.bus.client.response.JourneyPatternPointResponse;
import se.transport.bus.client.response.StopPoint;
import se.transport.bus.exception.BusTransportException;
import se.transport.bus.exception.InternalServerError;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class BusTransportService {

    private final TrafikLabWebClient trafikLabWebClient;

    public Flux<BusLine> findTopMostBusLines(Long queryLimit) {
        return trafikLabWebClient.getJourneyPatternPoints()
                .map(journeyPatternPointResponse -> findTopMostBusLines(journeyPatternPointResponse.getResponseData().getResult(), queryLimit))
                .flatMapIterable(busLines -> busLines);
    }

    private List<BusLine> findTopMostBusLines(List<JourneyPatternPointResponse> journeyPoints, Long queryLimit) {
        return journeyPoints
                .parallelStream()
                .collect(
                        Collectors.collectingAndThen(
                                Collectors.groupingBy(JourneyPatternPointResponse::getLineNumber, Collectors.groupingBy(JourneyPatternPointResponse::getDirectionCode)), // group by line number and direction
                                lineNumberGroup -> lineNumberGroup
                                        .entrySet()
                                        .stream()
                                        .sorted((o1, o2) -> Integer.compare(o2.getValue()
                                                        .values()
                                                        .parallelStream()
                                                        .map(List::size)
                                                        .reduce(Integer::sum).orElseThrow(() -> new InternalServerError("Error while grouping journey points")),
                                                o1.getValue().values()
                                                        .parallelStream()
                                                        .map(List::size)
                                                        .reduce(Integer::sum).orElseThrow(() -> new InternalServerError("Error while grouping journey points")))
                                        ) // sort reverse order
                                        .limit(queryLimit) // limit response
                                        .map(lineMap -> {
                                            Map<String, List<JourneyPatternPointResponse>> directionJourneyPatterns = lineMap.getValue();
                                            final AtomicInteger stopCount = new AtomicInteger();
                                            List<BusLine.Direction> directions = directionJourneyPatterns
                                                    .entrySet()
                                                    .stream()
                                                    .map(directionEntry -> {
                                                        int size = directionEntry.getValue().size();
                                                        stopCount.addAndGet(size);
                                                        return BusLine.Direction
                                                                .builder()
                                                                .directionNumber(directionEntry.getKey())
                                                                .totalStops(size)
                                                                .build();
                                                    })
                                                    .collect(Collectors.toList());

                                            return BusLine
                                                    .builder()
                                                    .lineNumber(lineMap.getKey())
                                                    .totalStops(stopCount.get())
                                                    .directions(directions)
                                                    .build();
                                        }))).collect(Collectors.toList());
    }

    public Mono<BusStops> getBusStopsForLine(String lineNumber) {
        return
                trafikLabWebClient.getJourneyPatternPoints()
                        .flatMap(response -> {
                            List<JourneyPatternPointResponse> journeyPatternPoints = response.getResponseData().getResult()
                                    .parallelStream()
                                    .filter(pattern -> pattern.getLineNumber().equals(lineNumber))
                                    .collect(Collectors.toList());

                            if (journeyPatternPoints.isEmpty()) {
                                throw new BusTransportException("Invalid Line Number");
                            }

                            return trafikLabWebClient.getStopPoints()
                                    .map(stopPointsResponse -> stopPointsResponse
                                            .getResponseData().getResult()
                                            .stream()
                                            .collect(Collectors.toMap(StopPoint::getStopPointNumber, stopPoint -> stopPoint)))
                                    .map(stopPointMap -> {
                                        Map<String, List<JourneyPatternPointResponse>> directionPatternMap = journeyPatternPoints
                                                .stream()
                                                .filter(journeyPattern -> journeyPattern.getLineNumber().equals(journeyPatternPoints.get(0).getLineNumber()))
                                                .collect(Collectors.groupingBy(JourneyPatternPointResponse::getDirectionCode));

                                        return BusStops.builder()
                                                .lineNumber(lineNumber)
                                                .busStops(
                                                        directionPatternMap
                                                                .entrySet()
                                                                .stream()
                                                                .collect(Collectors.toMap(Map.Entry::getKey,
                                                                        directionPatternEntry -> directionPatternEntry.getValue()
                                                                                .stream()
                                                                                .map(journeyPatternPointResponse -> stopPointMap.get(journeyPatternPointResponse.getJourneyPatternPointNumber()))
                                                                                .collect(Collectors.toList())
                                                                ))
                                                )
                                                .build();
                                    });
                        });
    }
}

