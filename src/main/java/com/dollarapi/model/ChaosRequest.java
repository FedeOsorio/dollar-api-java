package com.dollarapi.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Chaos simulation request")
public class ChaosRequest {

    @NotNull(message = "Chaos type is required")
    @Schema(description = "Chaos mode type", example = "OUTAGE", allowableValues = {"NONE", "OUTAGE", "TIMEOUT", "SLOW"})
    private ChaosMode type;
}
