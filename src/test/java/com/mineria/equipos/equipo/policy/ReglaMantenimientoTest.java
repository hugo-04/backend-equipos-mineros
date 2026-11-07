package com.mineria.equipos.equipo.policy;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.mineria.equipos.equipo.entity.Equipo;

/**
 * Prueba específica de la regla de mantenimiento:
 * requiere = (horometro - horometroUltimoMant) >= intervaloHoras del tipo.
 * Cubre justo debajo del intervalo, exactamente en el intervalo, por encima, y el reinicio del ciclo.
 */
class ReglaMantenimientoTest {

    private static final int INTERVALO_CAMION = 250;

    private final MantenimientoPolicy policy = new IntervaloMantenimientoPolicy();

    private boolean requiere(Equipo equipo) {
        return policy.requiereMantenimiento(equipo.getHorometro(), equipo.getHorometroUltimoMant(), INTERVALO_CAMION);
    }

    @Test
    @DisplayName("justo debajo del intervalo (249.9 h) NO requiere mantenimiento")
    void justoDebajoDelIntervalo() {
        // Arrange
        Equipo equipo = equipoConHorometro("249.9");

        // Act
        boolean requiere = requiere(equipo);

        // Assert
        assertThat(requiere).isFalse();
    }

    @Test
    @DisplayName("exactamente en el intervalo (250 h) SÍ requiere mantenimiento")
    void exactamenteEnElIntervalo() {
        // Arrange
        Equipo equipo = equipoConHorometro("250.0");

        // Act
        boolean requiere = requiere(equipo);

        // Assert
        assertThat(requiere).isTrue();
    }

    @Test
    @DisplayName("por encima del intervalo (250.1 h) SÍ requiere mantenimiento")
    void porEncimaDelIntervalo() {
        // Arrange
        Equipo equipo = equipoConHorometro("250.1");

        // Act
        boolean requiere = requiere(equipo);

        // Assert
        assertThat(requiere).isTrue();
    }

    @Test
    @DisplayName("un equipo recién creado en 0 h no requiere mantenimiento")
    void equipoNuevoEnCero() {
        // Arrange
        Equipo equipo = equipoConHorometro("0.0");

        // Act
        boolean requiere = requiere(equipo);

        // Assert
        assertThat(requiere).isFalse();
    }

    @Test
    @DisplayName("tras registrar un mantenimiento el ciclo se reinicia y deja de requerirlo")
    void reinicioTrasRegistrarMantenimiento() {
        // Arrange: equipo que ya pasó el intervalo
        Equipo vencido = equipoConHorometro("300.0");
        assertThat(requiere(vencido)).isTrue();

        // Act: se reinicia el ciclo (lo que hace el servicio al registrar el mantenimiento)
        vencido.reiniciarCicloMantenimiento();

        // Assert: el último mantenimiento queda en el horómetro actual
        assertThat(vencido.getHorometroUltimoMant()).isEqualByComparingTo("300.0");
        assertThat(requiere(vencido)).isFalse();
    }

    @Test
    @DisplayName("después del reinicio vuelve a requerirlo al completar otro intervalo")
    void nuevoCicloDespuesDelReinicio() {
        // Arrange: ciclo reiniciado en 300 h
        Equipo equipo = equipoConHorometro("300.0");
        equipo.reiniciarCicloMantenimiento();

        // Act: el equipo trabaja 249.9 h más y luego llega justo a 250 h del último mantenimiento
        equipo.actualizar("CAM-001", 1L, new BigDecimal("549.9"));
        boolean casiVencido = requiere(equipo);
        equipo.actualizar("CAM-001", 1L, new BigDecimal("550.0"));
        boolean vencido = requiere(equipo);

        // Assert
        assertThat(casiVencido).isFalse();
        assertThat(vencido).isTrue();
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

    private Equipo equipoConHorometro(String horometro) {
        Equipo equipo = new Equipo();
        equipo.setId(10L);
        equipo.setCodigo("CAM-001");
        equipo.setTipoId(1L);
        equipo.setHorometro(new BigDecimal(horometro));
        return equipo;
    }
}
