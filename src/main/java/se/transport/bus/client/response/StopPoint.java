package se.transport.bus.client.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

@Data
public class StopPoint implements Serializable {

    @JsonProperty("StopPointNumber")
    private String stopPointNumber;
    @JsonProperty("StopPointName")
    private String stopPointName;
    @JsonProperty("StopAreaNumber")
    private String stopAreaNumber;
    @JsonProperty("LocationNorthingCoordinate")
    private String locationNorthingCoordinate;
    @JsonProperty("LocationEastingCoordinate")
    private String locationEastingCoordinate;
    @JsonProperty("ZoneShortName")
    private String zoneShortName;
}
