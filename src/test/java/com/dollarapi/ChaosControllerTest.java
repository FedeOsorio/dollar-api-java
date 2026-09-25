package com.dollarapi;

import com.dollarapi.model.ChaosMode;
import com.dollarapi.service.ChaosService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ChaosControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ChaosService chaosService;

    @Test
    @DisplayName("POST /api/v1/chaos/mode?type=OUTAGE should set mode and reflect in health endpoint")
    void shouldSetChaosModeViaQueryParam() throws Exception {
        mockMvc.perform(post("/api/v1/chaos/mode?type=OUTAGE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.currentMode").value("OUTAGE"));

        assertThat(chaosService.getMode()).isEqualTo(ChaosMode.OUTAGE);

        // Check health endpoint reflects OUTAGE
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.upstream.chaosMode").value("OUTAGE"))
                .andExpect(jsonPath("$.upstream.status").value("DOWN"));

        // Reset back to NONE
        chaosService.setMode(ChaosMode.NONE);
    }

    @Test
    @DisplayName("POST /api/v1/chaos/mode with JSON body should update mode")
    void shouldSetChaosModeViaJsonBody() throws Exception {
        mockMvc.perform(post("/api/v1/chaos/mode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\": \"TIMEOUT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.currentMode").value("TIMEOUT"));

        assertThat(chaosService.getMode()).isEqualTo(ChaosMode.TIMEOUT);

        // Reset back to NONE
        chaosService.setMode(ChaosMode.NONE);
    }
}
