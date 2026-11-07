package com.mineria.equipos.equipo.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.mineria.equipos.shared.exception.HorometroInvalidoException;

class EquipoTest {

    private static final Instant AHORA = Instant.parse("2026-03-01T10:00:00Z");

    private Equipo equipo(String horometro) {
        Equipo equipo = new Equipo();
        equipo.setCodigo("CAM-001");
        equipo.setTipoId(1L);
        equipo.setHorometro(new BigDecimal(horometro));
        return equipo;
    }

    @Test
    @DisplayName("darDeAlta: arranca el ciclo de mantenimiento en 0 y guarda la fecha")
    void altaArrancaCicloEnCero() {
        // Arrange
        Equipo equipo = equipo("120.5");
        equipo.setHorometroUltimoMant(new BigDecimal("99"));

        // Act
        equipo.darDeAlta(AHORA);

        // Assert
        assertThat(equipo.getId()).isNull();
        assertThat(equipo.getHorometroUltimoMant()).isEqualByComparingTo("0");
        assertThat(equipo.getCreadoEn()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("un equipo nuevo parte con el ciclo en 0 aunque no se llame darDeAlta")
    void cicloPorDefecto() {
        // Act + Assert
        assertThat(new Equipo().getHorometroUltimoMant()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("darDeAlta: rechaza un horómetro negativo")
    void altaRechazaNegativo() {
        // Arrange
        Equipo equipo = equipo("-0.1");

        // Act + Assert
        assertThatThrownBy(() -> equipo.darDeAlta(AHORA))
                .isInstanceOf(HorometroInvalidoException.class)
                .hasMessageContaining("mayor o igual a 0");
    }

    @Test
    @DisplayName("darDeAlta: rechaza un horómetro nulo")
    void altaRechazaNulo() {
        // Arrange
        Equipo equipo = new Equipo();

        // Act + Assert
        assertThatThrownBy(() -> equipo.darDeAlta(AHORA)).isInstanceOf(HorometroInvalidoException.class);
    }

    @Test
    @DisplayName("actualizar: permite subir el horómetro y cambiar código y tipo")
    void actualizarPermiteSubir() {
        // Arrange
        Equipo equipo = equipo("100.0");

        // Act
        equipo.actualizar("EXC-009", 2L, new BigDecimal("150.0"));

        // Assert
        assertThat(equipo.getCodigo()).isEqualTo("EXC-009");
        assertThat(equipo.getTipoId()).isEqualTo(2L);
        assertThat(equipo.getHorometro()).isEqualByComparingTo("150.0");
    }

    @Test
    @DisplayName("actualizar: permite dejar el mismo horómetro")
    void actualizarPermiteIgual() {
        // Arrange
        Equipo equipo = equipo("100.0");

        // Act
        equipo.actualizar("CAM-001", 1L, new BigDecimal("100.0"));

        // Assert
        assertThat(equipo.getHorometro()).isEqualByComparingTo("100.0");
    }

    @Test
    @DisplayName("actualizar: NO permite bajar el horómetro y no cambia nada")
    void actualizarRechazaBajar() {
        // Arrange
        Equipo equipo = equipo("100.0");
        BigDecimal menor = new BigDecimal("99.9");

        // Act + Assert
        assertThatThrownBy(() -> equipo.actualizar("OTRO-1", 2L, menor))
                .isInstanceOf(HorometroInvalidoException.class)
                .hasMessageContaining("no puede bajar");
        assertThat(equipo.getCodigo()).isEqualTo("CAM-001");
        assertThat(equipo.getHorometro()).isEqualByComparingTo("100.0");
    }

    @Test
    @DisplayName("actualizar: rechaza un horómetro negativo")
    void actualizarRechazaNegativo() {
        // Arrange
        Equipo equipo = equipo("0.0");
        BigDecimal negativo = new BigDecimal("-1");

        // Act + Assert
        assertThatThrownBy(() -> equipo.actualizar("CAM-001", 1L, negativo))
                .isInstanceOf(HorometroInvalidoException.class);
    }

    @Test
    @DisplayName("validarHorometroNuevo: no lanza si el valor es igual o mayor")
    void validarPermiteIgualOMayor() {
        // Arrange
        Equipo equipo = equipo("100.0");

        // Act + Assert
        assertThatCode(() -> equipo.validarHorometroNuevo(new BigDecimal("100.0"))).doesNotThrowAnyException();
        assertThatCode(() -> equipo.validarHorometroNuevo(new BigDecimal("100.1"))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("reiniciarCicloMantenimiento: el horómetro actual pasa a ser el del último mantenimiento")
    void reiniciaElCiclo() {
        // Arrange
        Equipo equipo = equipo("310.5");

        // Act
        BigDecimal reiniciadoEn = equipo.reiniciarCicloMantenimiento();

        // Assert
        assertThat(reiniciadoEn).isEqualByComparingTo("310.5");
        assertThat(equipo.getHorometroUltimoMant()).isEqualByComparingTo("310.5");
    }
}
