package com.dollarapi.model;

import com.fasterxml.jackson.annotation.JsonFormat;
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
@Schema(description = "Chaos simulation response")
public class ChaosResponse {

    @Schema(description = "Execution status", example = "SUCCESS")
    private String status;

    @Schema(description = "Informative message", example = "Chaos mode updated to OUTAGE")
    private String message;

    @Schema(description = "Current active chaos mode", example = "OUTAGE")
    private ChaosMode currentMode;

    @Schema(description = "Timestamp of mode transition")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant timestamp;
}
