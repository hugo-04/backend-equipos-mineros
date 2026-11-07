package com.mineria.equipos.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.mineria.equipos.domain.exception.HorometroInvalidoException;

class EquipoTest {

    private static final Instant AHORA = Instant.parse("2026-03-01T10:00:00Z");

    private final TipoEquipo camion = TipoEquipo.builder().id(1L).nombre("Camión").intervaloHoras(250).build();
    private final TipoEquipo excavadora = TipoEquipo.builder().id(2L).nombre("Excavadora").intervaloHoras(500).build();

    @Test
    @DisplayName("nuevo: arranca el ciclo de mantenimiento en 0")
    void nuevoArrancaCicloEnCero() {
        // Arrange + Act
        Equipo equipo = Equipo.nuevo("CAM-001", camion, new BigDecimal("120.5"), AHORA);

        // Assert
        assertThat(equipo.getId()).isNull();
        assertThat(equipo.getCodigo()).isEqualTo("CAM-001");
        assertThat(equipo.getHorometro()).isEqualByComparingTo("120.5");
        assertThat(equipo.getHorometroUltimoMant()).isEqualByComparingTo("0");
        assertThat(equipo.getCreadoEn()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("nuevo: rechaza un horómetro negativo")
    void nuevoRechazaNegativo() {
        // Arrange
        BigDecimal negativo = new BigDecimal("-0.1");

        // Act + Assert
        assertThatThrownBy(() -> Equipo.nuevo("CAM-001", camion, negativo, AHORA))
                .isInstanceOf(HorometroInvalidoException.class)
                .hasMessageContaining("mayor o igual a 0");
    }

    @Test
    @DisplayName("nuevo: rechaza un horómetro nulo")
    void nuevoRechazaNulo() {
        // Act + Assert
        assertThatThrownBy(() -> Equipo.nuevo("CAM-001", camion, null, AHORA))
                .isInstanceOf(HorometroInvalidoException.class);
    }

    @Test
    @DisplayName("actualizar: permite subir el horómetro y cambiar código y tipo")
    void actualizarPermiteSubir() {
        // Arrange
        Equipo equipo = Equipo.nuevo("CAM-001", camion, new BigDecimal("100.0"), AHORA);

        // Act
        Equipo actualizado = equipo.actualizar("EXC-009", excavadora, new BigDecimal("150.0"));

        // Assert: el original no cambia (es inmutable) y la copia trae los datos nuevos
        assertThat(equipo.getCodigo()).isEqualTo("CAM-001");
        assertThat(actualizado.getCodigo()).isEqualTo("EXC-009");
        assertThat(actualizado.getTipo()).isEqualTo(excavadora);
        assertThat(actualizado.getHorometro()).isEqualByComparingTo("150.0");
    }

    @Test
    @DisplayName("actualizar: permite dejar el mismo horómetro")
    void actualizarPermiteIgual() {
        // Arrange
        Equipo equipo = Equipo.nuevo("CAM-001", camion, new BigDecimal("100.0"), AHORA);

        // Act
        Equipo actualizado = equipo.actualizar("CAM-001", camion, new BigDecimal("100.0"));

        // Assert
        assertThat(actualizado.getHorometro()).isEqualByComparingTo("100.0");
    }

    @Test
    @DisplayName("actualizar: NO permite bajar el horómetro")
    void actualizarRechazaBajar() {
        // Arrange
        Equipo equipo = Equipo.nuevo("CAM-001", camion, new BigDecimal("100.0"), AHORA);
        BigDecimal menor = new BigDecimal("99.9");

        // Act + Assert
        assertThatThrownBy(() -> equipo.actualizar("CAM-001", camion, menor))
                .isInstanceOf(HorometroInvalidoException.class)
                .hasMessageContaining("no puede bajar");
    }

    @Test
    @DisplayName("actualizar: rechaza un horómetro negativo")
    void actualizarRechazaNegativo() {
        // Arrange
        Equipo equipo = Equipo.nuevo("CAM-001", camion, new BigDecimal("0.0"), AHORA);
        BigDecimal negativo = new BigDecimal("-1");

        // Act + Assert
        assertThatThrownBy(() -> equipo.actualizar("CAM-001", camion, negativo))
                .isInstanceOf(HorometroInvalidoException.class);
    }
}
