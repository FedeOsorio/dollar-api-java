package com.dollarapi.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standard API Response Wrapper")
public class ApiResponse<T> {

    @Schema(description = "Business status of the response", example = "SUCCESS")
    private String status;

    @Schema(description = "Cache header diagnostic indicator", example = "HIT", allowableValues = {"HIT", "MISS", "STALE_FALLBACK"})
    private String cacheStatus;

    @Schema(description = "Timestamp of response generation")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant timestamp;

    @Schema(description = "Payload data")
    private T data;

    @Schema(description = "Optional message for error, warning, or fallback context", example = "Served from stale cache due to upstream outage")
    private String message;

    public static <T> ApiResponse<T> success(T data, String cacheStatus) {
        return ApiResponse.<T>builder()
                .status("SUCCESS")
                .cacheStatus(cacheStatus)
                .timestamp(Instant.now())
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> fallback(T data, String message) {
        return ApiResponse.<T>builder()
                .status("STALE_FALLBACK")
                .cacheStatus("STALE_FALLBACK")
                .timestamp(Instant.now())
                .data(data)
                .message(message)
                .build();
    }

    public static <T> ApiResponse<T> error(String status, String message) {
        return ApiResponse.<T>builder()
                .status(status)
                .timestamp(Instant.now())
                .message(message)
                .build();
    }
}
