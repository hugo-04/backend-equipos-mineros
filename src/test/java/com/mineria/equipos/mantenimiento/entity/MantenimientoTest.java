package com.mineria.equipos.mantenimiento.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MantenimientoTest {

    @Test
    @DisplayName("registrarPara: guarda el horómetro del equipo, el equipo, las notas y la fecha")
    void registrarParaCopiaElHorometroDelEquipo() {
        // Arrange
        Instant fecha = Instant.parse("2026-05-01T08:30:00Z");

        // Act
        Mantenimiento mantenimiento = Mantenimiento.registrarPara(7L, new BigDecimal("310.5"), "Cambio de aceite", fecha);

        // Assert
        assertThat(mantenimiento.getId()).isNull();
        assertThat(mantenimiento.getEquipoId()).isEqualTo(7L);
        assertThat(mantenimiento.getHorometroAlRegistrar()).isEqualByComparingTo("310.5");
        assertThat(mantenimiento.getNotas()).isEqualTo("Cambio de aceite");
        assertThat(mantenimiento.getFecha()).isEqualTo(fecha);
    }
}
