package se.transport.bus.api.model;

import se.transport.bus.client.response.StopPoint;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
@Builder(toBuilder = true)
public class BusStops implements Serializable {

    private String lineNumber;
    private Map<String, List<StopPoint>> busStops;


}
