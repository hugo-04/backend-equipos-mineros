package com.mineria.equipos.shared.health;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.mineria.equipos.generated.model.HealthResponse;

/**
 * Que la respuesta del /health lleve el estado, la versión y desde cuándo corre.
 */
class HealthMapperTest {

    private final HealthMapper mapper = new HealthMapperImpl();

    @Test
    @DisplayName("arma la respuesta con el estado, la versión y el momento de arranque")
    void health() {
        // Arrange
        Instant arranque = Instant.parse("2026-11-09T18:49:43Z");

        // Act
        HealthResponse up = mapper.toResponse(true, "abc123", arranque);
        HealthResponse down = mapper.toResponse(false, "abc123", arranque);

        // Assert
        assertThat(up.getStatus()).isEqualTo(HealthResponse.StatusEnum.UP);
        assertThat(up.getVersion()).isEqualTo("abc123");
        assertThat(up.getIniciadoEn()).isEqualTo(arranque);
        assertThat(down.getStatus()).isEqualTo(HealthResponse.StatusEnum.DOWN);
        assertThat(down.getVersion()).isEqualTo("abc123");
        assertThat(down.getIniciadoEn()).isEqualTo(arranque);
    }
}
