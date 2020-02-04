package se.transport.bus.client.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class BaseResponse<T> implements Serializable {

    @JsonProperty("ExecutionTime")
    private Integer executionTime;
    @JsonProperty("Message")
    private String message;
    @JsonProperty("StatusCode")
    private Integer statusCode;
    @JsonProperty("ResponseData")
    private ResponseData<T> responseData;

    @Data
    public static class ResponseData<T> implements Serializable {

        @JsonProperty("Version")
        private String version;
        @JsonProperty("Type")
        private String type;
        @JsonProperty("Result")
        private List<T> result;

    }
}
