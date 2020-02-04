package se.transport.bus.service;

import se.transport.bus.client.TrafikLabWebClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class BusTransportServiceBkp {

    private final TrafikLabWebClient trafikLabWebClient;

    /*public Flux<BusLine> findTopMostBusLines(Long queryLimit) {
        return trafikLabWebClient.getJourneyPatternPoints()
                .map(journeyPatternPointResponse -> findTopMostBusLines(journeyPatternPointResponse.getResponseData().getResult(), queryLimit))
                .flatMapIterable(busLines -> busLines);
    }

    private List<BusLine> findTopMostBusLines(List<JourneyPatternPointResponse> journeyPoints, Long queryLimit) {
        return journeyPoints
                .parallelStream()
                .collect(
                        Collectors.collectingAndThen(
                                Collectors.groupingBy(JourneyPatternPointResponse::getLineNumber), // group by line number
                                lineNumberGroup -> lineNumberGroup
                                        .entrySet()
                                        .stream()
                                        .sorted((o1, o2) -> Integer.compare(o2.getValue().size(), o1.getValue().size())) // sort reverse order
                                        .limit(queryLimit) // limit response
                                        .map(journeyPatternEntry -> {
                                            List<JourneyPatternPointResponse> journeyPatterns = journeyPatternEntry.getValue();
                                            return journeyPatterns.stream()
                                                    .collect(Collectors.collectingAndThen(
                                                            Collectors.groupingBy(JourneyPatternPointResponse::getDirectionCode), // group by direction
                                                            directionGroup -> {
                                                                List<BusLine.Direction> directions = directionGroup.entrySet()
                                                                        .stream()
                                                                        .map(directionEntry -> BusLine.Direction
                                                                                .builder()
                                                                                .directionNumber(directionEntry.getKey())
                                                                                .totalStops(directionEntry.getValue().size())
                                                                                .build()).collect(Collectors.toList());

                                                                return BusLine
                                                                        .builder()
                                                                        .lineNumber(journeyPatternEntry.getKey())
                                                                        .totalStops(journeyPatternEntry.getValue().size())
                                                                        .directions(directions)
                                                                        .build();
                                                            }));
                                        }))).collect(Collectors.toList());
    }

    public Mono<BusStops> getBusStopsForLine(String lineNumber) {
        return
                trafikLabWebClient.getBusLines()
                        .flatMap(response -> {
                            LineResponse busLine = response.getResponseData().getResult()
                                    .parallelStream()
                                    .filter(line -> line.getLineNumber().equals(lineNumber))
                                    .findFirst()
                                    .orElseThrow(() -> new BusTransportException("Invalid Line Number"));

                            return Mono.zip(trafikLabWebClient.getJourneyPatternPoints(), trafikLabWebClient.getStopPoints(),
                                    (journeyPatternPoints, stopPoints) -> {
                                        // filter patterns
                                        Map<String, List<JourneyPatternPointResponse>> directionPatternMap = journeyPatternPoints
                                                .getResponseData()
                                                .getResult()
                                                .stream()
                                                .filter(journeyPattern -> journeyPattern.getLineNumber().equals(busLine.getLineNumber()))
                                                .collect(Collectors.groupingBy(JourneyPatternPointResponse::getDirectionCode));

                                        Map<String, StopPoint> stopPointMap = constructStopPointMap(stopPoints.getResponseData().getResult());

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

    private Map<String, StopPoint> constructStopPointMap(List<StopPoint> result) {
        return result.stream()
                .collect(Collectors.toMap(StopPoint::getStopPointNumber, stopPoint -> stopPoint));
    }*/
}

