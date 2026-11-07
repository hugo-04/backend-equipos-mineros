package com.mineria.equipos.domain.rule;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.mineria.equipos.domain.model.Equipo;
import com.mineria.equipos.domain.model.TipoEquipo;

/**
 * Prueba específica de la regla de mantenimiento:
 * requiere = (horometro - horometroUltimoMant) >= intervaloHoras del tipo.
 * Cubre justo debajo del intervalo, exactamente en el intervalo, por encima, y el reinicio del ciclo.
 */
class ReglaMantenimientoTest {

    private static final int INTERVALO_CAMION = 250;

    private final MantenimientoPolicy policy = new IntervaloMantenimientoPolicy();
    private final TipoEquipo camion = TipoEquipo.builder().id(1L).nombre("Camión").intervaloHoras(INTERVALO_CAMION).build();

    @Test
    @DisplayName("justo debajo del intervalo (249.9 h) NO requiere mantenimiento")
    void justoDebajoDelIntervalo() {
        // Arrange
        Equipo equipo = equipoConHorometro("249.9");

        // Act
        boolean requiere = equipo.requiereMantenimiento(policy);

        // Assert
        assertThat(requiere).isFalse();
    }

    @Test
    @DisplayName("exactamente en el intervalo (250 h) SÍ requiere mantenimiento")
    void exactamenteEnElIntervalo() {
        // Arrange
        Equipo equipo = equipoConHorometro("250.0");

        // Act
        boolean requiere = equipo.requiereMantenimiento(policy);

        // Assert
        assertThat(requiere).isTrue();
    }

    @Test
    @DisplayName("por encima del intervalo (250.1 h) SÍ requiere mantenimiento")
    void porEncimaDelIntervalo() {
        // Arrange
        Equipo equipo = equipoConHorometro("250.1");

        // Act
        boolean requiere = equipo.requiereMantenimiento(policy);

        // Assert
        assertThat(requiere).isTrue();
    }

    @Test
    @DisplayName("un equipo recién creado en 0 h no requiere mantenimiento")
    void equipoNuevoEnCero() {
        // Arrange
        Equipo equipo = equipoConHorometro("0.0");

        // Act
        boolean requiere = equipo.requiereMantenimiento(policy);

        // Assert
        assertThat(requiere).isFalse();
    }

    @Test
    @DisplayName("tras registrar un mantenimiento el ciclo se reinicia y deja de requerirlo")
    void reinicioTrasRegistrarMantenimiento() {
        // Arrange: equipo que ya pasó el intervalo
        Equipo vencido = equipoConHorometro("300.0");
        assertThat(vencido.requiereMantenimiento(policy)).isTrue();

        // Act: se reinicia el ciclo (lo que hace el caso de uso al registrar el mantenimiento)
        Equipo reiniciado = vencido.reiniciarCicloMantenimiento();

        // Assert: el último mantenimiento queda en el horómetro actual
        assertThat(reiniciado.getHorometroUltimoMant()).isEqualByComparingTo("300.0");
        assertThat(reiniciado.requiereMantenimiento(policy)).isFalse();
    }

    @Test
    @DisplayName("después del reinicio vuelve a requerirlo al completar otro intervalo")
    void nuevoCicloDespuesDelReinicio() {
        // Arrange: ciclo reiniciado en 300 h
        Equipo reiniciado = equipoConHorometro("300.0").reiniciarCicloMantenimiento();

        // Act: el equipo trabaja 249.9 h más y luego llega justo a 250 h del último mantenimiento
        Equipo casiVencido = reiniciado.actualizar("CAM-001", camion, new BigDecimal("549.9"));
        Equipo vencido = reiniciado.actualizar("CAM-001", camion, new BigDecimal("550.0"));

        // Assert
        assertThat(casiVencido.requiereMantenimiento(policy)).isFalse();
        assertThat(vencido.requiereMantenimiento(policy)).isTrue();
    }

    private Equipo equipoConHorometro(String horometro) {
        return Equipo.builder()
                .id(10L)
                .codigo("CAM-001")
                .tipo(camion)
                .horometro(new BigDecimal(horometro))
                .horometroUltimoMant(BigDecimal.ZERO)
                .creadoEn(Instant.parse("2026-01-01T00:00:00Z"))
                .build();
    }
}
