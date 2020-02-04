package se.transport.bus.api.model;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@Builder(toBuilder = true)
public class BusLine implements Serializable {

    private String lineNumber;
    private Integer totalStops;
    private List<Direction> directions;

    @Data
    @Builder(toBuilder = true)
    public static class Direction implements Serializable {
        private String directionNumber;
        private Integer totalStops;
    }
}
