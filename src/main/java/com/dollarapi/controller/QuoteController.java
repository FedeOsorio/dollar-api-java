package com.dollarapi.controller;

import com.dollarapi.model.ApiResponse;
import com.dollarapi.model.Quote;
import com.dollarapi.model.QuoteResult;
import com.dollarapi.service.QuoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/quotes")
@RequiredArgsConstructor
@Tag(name = "Quotes", description = "Operations for retrieving real-time Argentine Dollar exchange rates")
public class QuoteController {

    private static final String HEADER_CACHE_STATUS = "X-Cache-Status";

    private final QuoteService quoteService;

    @GetMapping
    @Operation(summary = "List all dollar quotes",
            description = "Retrieves live quotes for all dollar types (Oficial, Blue, MEP, CCL, Tarjeta, Cripto, Mayorista). "
                    + "Features automatic Circuit Breaker fallback to stale cache or emergency snapshot when external service is degraded.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Successfully retrieved quotes",
                    headers = @Header(name = HEADER_CACHE_STATUS, description = "Cache indicator: HIT, MISS, or STALE_FALLBACK", schema = @Schema(type = "string")),
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ResponseEntity<ApiResponse<List<Quote>>> getAllQuotes() {
        QuoteResult result = quoteService.getAllQuotes();

        HttpHeaders headers = new HttpHeaders();
        headers.add(HEADER_CACHE_STATUS, result.getCacheStatus());

        ApiResponse<List<Quote>> response = "STALE_FALLBACK".equals(result.getCacheStatus())
                ? ApiResponse.fallback(result.getQuotes(), result.getMessage())
                : ApiResponse.success(result.getQuotes(), result.getCacheStatus());

        return ResponseEntity.ok()
                .headers(headers)
                .body(response);
    }

    @GetMapping("/{code}")
    @Operation(summary = "Get single quote by code",
            description = "Retrieves current exchange rate for a specific dollar type by code (e.g. blue, oficial, mep, ccl, tarjeta, cripto).")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Quote found successfully",
                    headers = @Header(name = HEADER_CACHE_STATUS, description = "Cache indicator: HIT, MISS, or STALE_FALLBACK", schema = @Schema(type = "string")),
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Quote code not found")
    })
    public ResponseEntity<ApiResponse<Quote>> getQuoteByCode(
            @Parameter(description = "Quote code identifier (e.g. 'blue', 'oficial', 'mep', 'ccl')", example = "blue")
            @PathVariable String code) {

        QuoteResult result = quoteService.getQuoteByCode(code);
        Quote singleQuote = result.getQuotes().getFirst();

        HttpHeaders headers = new HttpHeaders();
        headers.add(HEADER_CACHE_STATUS, result.getCacheStatus());

        ApiResponse<Quote> response = "STALE_FALLBACK".equals(result.getCacheStatus())
                ? ApiResponse.fallback(singleQuote, result.getMessage())
                : ApiResponse.success(singleQuote, result.getCacheStatus());

        return ResponseEntity.ok()
                .headers(headers)
                .body(response);
    }
}
