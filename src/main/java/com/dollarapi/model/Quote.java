package com.dollarapi.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Exchange rate quote")
public class Quote implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "Quote code identifier", example = "blue")
    private String code;

    @Schema(description = "Human-readable quote name", example = "Blue")
    private String name;

    @Schema(description = "Purchase exchange rate in ARS", example = "1540.0")
    private Double buy;

    @Schema(description = "Selling exchange rate in ARS", example = "1560.0")
    private Double sell;

    @Schema(description = "Currency code", example = "USD")
    private String currency;

    @Schema(description = "Last update timestamp from source")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant updatedAt;
}
