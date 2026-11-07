package com.mineria.equipos.shared.health;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.mineria.equipos.generated.model.HealthResponse;
import com.mineria.equipos.testsupport.ControllerTest;

/**
 * El /health: 200 con la versión cuando la base responde y 503 cuando no, sin pedir token.
 */
@ControllerTest(HealthController.class)
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HealthService healthService;

    @Test
    @DisplayName("GET /health: 200 UP y la versión, público")
    void healthUp() throws Exception {
        // Arrange
        when(healthService.verificar()).thenReturn(
                new HealthResponse().status(HealthResponse.StatusEnum.UP).version("test"));

        // Act + Assert
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.version").value("test"));
    }

    @Test
    @DisplayName("GET /health: 503 DOWN si la base de datos no responde")
    void healthDown() throws Exception {
        // Arrange
        when(healthService.verificar()).thenReturn(
                new HealthResponse().status(HealthResponse.StatusEnum.DOWN).version("test"));

        // Act + Assert
        mockMvc.perform(get("/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("DOWN"));
    }
}
