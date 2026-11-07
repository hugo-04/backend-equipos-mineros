package com.mineria.equipos.equipo.policy;

import static org.assertj.core.api.Assertions.assertThat;

import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mineria.equipos.equipo.entity.Equipo;
import com.mineria.equipos.equipo.repository.EquipoRepository;
import com.mineria.equipos.equipo.service.impl.EquipoServiceImpl;

/**
 * Prueba específica de la regla de mantenimiento:
 * requiere = (horometro - horometroUltimoMant) >= intervaloHoras del tipo.
 * Cubre justo debajo del intervalo, exactamente en el intervalo, por encima, y el reinicio del ciclo.
 */
@ExtendWith(MockitoExtension.class)
class ReglaMantenimientoTest {

    private static final int INTERVALO_CAMION = 250;

    private final MantenimientoPolicy policy = new IntervaloMantenimientoPolicy();

    @Mock
    private EquipoRepository repository;

    private EquipoServiceImpl service;

    @BeforeEach
    void setUp() {
        // Solo se usa el reinicio del ciclo, que únicamente necesita el repositorio
        service = new EquipoServiceImpl(repository, null, null, null);
    }

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
        when(repository.findById(10L)).thenReturn(Optional.of(vencido));

        // Act: el servicio de equipos reinicia el ciclo (lo que ocurre al registrar el mantenimiento)
        service.reiniciarCiclo(10L);

        // Assert: el último mantenimiento queda en el horómetro actual
        assertThat(vencido.getHorometroUltimoMant()).isEqualByComparingTo("300.0");
        assertThat(requiere(vencido)).isFalse();
    }

    @Test
    @DisplayName("después del reinicio vuelve a requerirlo al completar otro intervalo")
    void nuevoCicloDespuesDelReinicio() {
        // Arrange: ciclo reiniciado en 300 h
        Equipo equipo = equipoConHorometro("300.0");
        when(repository.findById(10L)).thenReturn(Optional.of(equipo));
        service.reiniciarCiclo(10L);

        // Act: el equipo trabaja 249.9 h más y luego llega justo a 250 h del último mantenimiento
        equipo.setHorometro(new BigDecimal("549.9"));
        boolean casiVencido = requiere(equipo);
        equipo.setHorometro(new BigDecimal("550.0"));
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
        equipo.setHorometroUltimoMant(BigDecimal.ZERO);
        return equipo;
    }

    @ParameterizedTest(name = "horómetro {0}, último mant. {1}: desde={2}, restantes={3}")
    @CsvSource({
            "100.0, 0.0, 100.0, 150.0",
            "249.9, 0.0, 249.9, 0.1",
            "250.0, 0.0, 250.0, 0.0",
            "260.0, 0.0, 260.0, 0.0",
            "260.0, 260.0, 0.0, 250.0",
            "510.0, 260.0, 250.0, 0.0"})
    @DisplayName("horasDesdeUltimoMant y horasRestantes: nunca negativas y el ciclo se cuenta desde el último mantenimiento")
    void horasDesdeYRestantes(String horometro, String ultimoMant, String desde, String restantes) {
        // Arrange
        BigDecimal h = new BigDecimal(horometro);
        BigDecimal u = new BigDecimal(ultimoMant);

        // Act
        BigDecimal horasDesde = policy.horasDesdeUltimoMant(h, u);
        BigDecimal horasRestantes = policy.horasRestantes(h, u, INTERVALO_CAMION);

        // Assert
        assertThat(horasDesde).isEqualByComparingTo(desde);
        assertThat(horasRestantes).isEqualByComparingTo(restantes);
        assertThat(horasRestantes.signum()).isGreaterThanOrEqualTo(0);
    }
}
