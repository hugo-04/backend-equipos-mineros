package com.mineria.equipos.shared.health;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.mineria.equipos.generated.model.HealthResponse;

/**
 * Que la respuesta del /health lleve el estado y la versión.
 */
class HealthMapperTest {

    private final HealthMapper mapper = new HealthMapperImpl();

    @Test
    @DisplayName("arma la respuesta con el estado y la versión")
    void health() {
        // Act
        HealthResponse up = mapper.toResponse(true, "abc123");
        HealthResponse down = mapper.toResponse(false, "abc123");

        // Assert
        assertThat(up.getStatus()).isEqualTo(HealthResponse.StatusEnum.UP);
        assertThat(up.getVersion()).isEqualTo("abc123");
        assertThat(down.getStatus()).isEqualTo(HealthResponse.StatusEnum.DOWN);
        assertThat(down.getVersion()).isEqualTo("abc123");
    }
}
