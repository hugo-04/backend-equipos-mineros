package com.mineria.equipos.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MantenimientoTest {

    @Test
    @DisplayName("registrarPara: guarda el horómetro actual del equipo y las notas")
    void registrarParaCopiaElHorometroDelEquipo() {
        // Arrange
        TipoEquipo tipo = TipoEquipo.builder().id(1L).nombre("Camión").intervaloHoras(250).build();
        Equipo equipo = Equipo.builder().id(7L).codigo("CAM-007").tipo(tipo)
                .horometro(new BigDecimal("310.5")).horometroUltimoMant(BigDecimal.ZERO).build();
        Instant fecha = Instant.parse("2026-05-01T08:30:00Z");

        // Act
        Mantenimiento mantenimiento = Mantenimiento.registrarPara(equipo, "Cambio de aceite", fecha);

        // Assert
        assertThat(mantenimiento.getId()).isNull();
        assertThat(mantenimiento.getEquipoId()).isEqualTo(7L);
        assertThat(mantenimiento.getHorometroAlRegistrar()).isEqualByComparingTo("310.5");
        assertThat(mantenimiento.getNotas()).isEqualTo("Cambio de aceite");
        assertThat(mantenimiento.getFecha()).isEqualTo(fecha);
    }

    @ParameterizedTest(name = "requiere={0} -> {1}")
    @CsvSource({"true,Requiere mantenimiento", "false,OK"})
    @DisplayName("EstadoMantenimiento: etiqueta legible según el booleano")
    void estadoLegible(boolean requiere, String etiqueta) {
        // Act
        EstadoMantenimiento estado = EstadoMantenimiento.de(requiere);

        // Assert
        assertThat(estado.getEtiqueta()).isEqualTo(etiqueta);
    }
}
