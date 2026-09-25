package com.dollarapi;

import com.dollarapi.exception.QuoteNotFoundException;
import com.dollarapi.model.Quote;
import com.dollarapi.model.QuoteResult;
import com.dollarapi.service.QuoteService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class QuoteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private QuoteService quoteService;

    @Test
    @DisplayName("GET /api/v1/quotes should return 200 with X-Cache-Status header and quotes list")
    void shouldReturnQuotesListWithHeader() throws Exception {
        List<Quote> quotes = List.of(
                Quote.builder().code("blue").name("Blue").buy(1540.0).sell(1560.0).currency("USD").updatedAt(Instant.now()).build(),
                Quote.builder().code("oficial").name("Oficial").buy(1490.0).sell(1540.0).currency("USD").updatedAt(Instant.now()).build()
        );

        when(quoteService.getAllQuotes()).thenReturn(new QuoteResult(quotes, "HIT"));

        mockMvc.perform(get("/api/v1/quotes")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Cache-Status", "HIT"))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.cacheStatus").value("HIT"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].code").value("blue"))
                .andExpect(jsonPath("$.data[1].code").value("oficial"));
    }

    @Test
    @DisplayName("GET /api/v1/quotes/blue should return single quote")
    void shouldReturnSingleQuote() throws Exception {
        Quote blueQuote = Quote.builder().code("blue").name("Blue").buy(1540.0).sell(1560.0).currency("USD").updatedAt(Instant.now()).build();
        when(quoteService.getQuoteByCode("blue")).thenReturn(new QuoteResult(List.of(blueQuote), "MISS"));

        mockMvc.perform(get("/api/v1/quotes/blue")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Cache-Status", "MISS"))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.cacheStatus").value("MISS"))
                .andExpect(jsonPath("$.data.code").value("blue"))
                .andExpect(jsonPath("$.data.sell").value(1560.0));
    }

    @Test
    @DisplayName("GET /api/v1/quotes/invalid should return 404 NOT_FOUND")
    void shouldReturn404ForUnknownQuote() throws Exception {
        when(quoteService.getQuoteByCode("nonexistent")).thenThrow(new QuoteNotFoundException("nonexistent"));

        mockMvc.perform(get("/api/v1/quotes/nonexistent")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Quote not found for code: nonexistent"));
    }
}
