package com.dollarapi.controller;

import com.dollarapi.model.ApiResponse;
import com.dollarapi.model.ChaosMode;
import com.dollarapi.model.ChaosRequest;
import com.dollarapi.model.ChaosResponse;
import com.dollarapi.service.ChaosService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/chaos")
@RequiredArgsConstructor
@Tag(name = "Chaos Simulation", description = "Test Circuit Breaker degradation, retries, and fallback behaviors dynamically")
public class ChaosController {

    private final ChaosService chaosService;

    @PostMapping("/mode")
    @Operation(summary = "Set chaos simulation mode",
            description = "Simulate real-world external provider failure conditions to verify Circuit Breaker transitions "
                    + "(CLOSED -> OPEN -> HALF_OPEN) and fallback to stale cache. Modes: NONE, OUTAGE (HTTP 503), TIMEOUT (>2s delay), SLOW (1.5s delay).")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Chaos mode configured successfully",
                    content = @Content(schema = @Schema(implementation = ChaosResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid chaos mode provided")
    })
    public ResponseEntity<?> setChaosMode(
            @Parameter(description = "Chaos mode type via query parameter", example = "OUTAGE")
            @RequestParam(required = false) ChaosMode type,
            @RequestBody(required = false) ChaosRequest body) {

        ChaosMode targetMode = type != null ? type : (body != null ? body.getType() : null);

        if (targetMode == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("BAD_REQUEST", "Please supply 'type' as a query param (e.g. ?type=OUTAGE) or in the JSON request body"));
        }

        ChaosResponse response = chaosService.setMode(targetMode);
        return ResponseEntity.ok(response);
    }
}
