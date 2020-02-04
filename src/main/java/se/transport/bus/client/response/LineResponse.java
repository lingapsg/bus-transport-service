package se.transport.bus.client.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

@Data
public class LineResponse implements Serializable {

    @JsonProperty("LineNumber")
    private String lineNumber;
    @JsonProperty("LineDesignation")
    private String lineDesignation;
    @JsonProperty("DefaultTransportMode")
    private String defaultTransportMode;
    @JsonProperty("DefaultTransportModeCode")
    private String defaultTransportModeCode;
    @JsonProperty("LastModifiedUtcDateTime")
    private String lastModifiedUtcDateTime;
    @JsonProperty("ExistsFromDate")
    private String existsFromDate;
}
