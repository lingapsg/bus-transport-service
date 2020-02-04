package se.transport.bus.client.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

@Data
public class JourneyPatternPointResponse implements Serializable {

    @JsonProperty("LineNumber")
    private String lineNumber;
    @JsonProperty("DirectionCode")
    private String directionCode;
    @JsonProperty("JourneyPatternPointNumber")
    private String journeyPatternPointNumber;
    @JsonProperty("LastModifiedUtcDateTime")
    private String lastModifiedUtcDateTime;
    @JsonProperty("ExistsFromDate")
    private String existsFromDate;
}
