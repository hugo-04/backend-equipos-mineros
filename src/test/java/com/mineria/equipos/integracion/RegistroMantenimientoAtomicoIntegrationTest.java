package com.mineria.equipos.integracion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import com.mineria.equipos.equipo.service.EquipoService;
import com.mineria.equipos.generated.model.EquipoRequest;
import com.mineria.equipos.generated.model.EquipoResponse;
import com.mineria.equipos.generated.model.MantenimientoRequest;
import com.mineria.equipos.generated.model.MantenimientoResponse;
import com.mineria.equipos.mantenimiento.service.MantenimientoService;

/**
 * Comprueba la regla entre módulos: registrar un mantenimiento (módulo mantenimiento) reinicia el ciclo del
 * equipo (módulo equipo) dentro de UNA sola transacción. Sin @Transactional en la prueba para que el
 * rollback sea real.
 */
@SpringBootTest
@ActiveProfiles("test")
class RegistroMantenimientoAtomicoIntegrationTest {

    @Autowired
    private EquipoService equipoService;
    @Autowired
    private MantenimientoService mantenimientoService;

    private Long crearEquipo(String codigo) {
        return equipoService.crear(new EquipoRequest(codigo, 1L, new BigDecimal("300.0"))).getId();
    }

    @Test
    @DisplayName("registrar un mantenimiento reinicia el ciclo del equipo y guarda el historial")
    void registraYReinicia() {
        // Arrange
        Long id = crearEquipo("ATOM-OK-1");

        // Act
        MantenimientoResponse registrado = mantenimientoService.registrar(id, new MantenimientoRequest().notas("ok"));

        // Assert
        EquipoResponse equipo = equipoService.obtener(id);
        assertThat(registrado.getHorometroAlRegistrar()).isEqualByComparingTo("300.0");
        assertThat(equipo.getHorometroUltimoMant()).isEqualByComparingTo("300.0");
        assertThat(equipo.getRequiereMantenimiento()).isFalse();
        assertThat(mantenimientoService.listarPorEquipo(id)).hasSize(1);
    }

    @Test
    @DisplayName("si falla guardar el historial, el ciclo del equipo NO queda reiniciado (rollback conjunto)")
    void siFallaElHistorialNoSeReinicia() {
        // Arrange: notas más largas que la columna (500) hacen fallar el INSERT del historial
        Long id = crearEquipo("ATOM-FALLA-1");
        MantenimientoRequest demasiadoLargo = new MantenimientoRequest().notas("x".repeat(600));

        // Act + Assert
        assertThatThrownBy(() -> mantenimientoService.registrar(id, demasiadoLargo))
                .isInstanceOf(DataIntegrityViolationException.class);
        EquipoResponse equipo = equipoService.obtener(id);
        assertThat(equipo.getHorometroUltimoMant()).isEqualByComparingTo("0");
        assertThat(equipo.getRequiereMantenimiento()).isTrue();
        assertThat(mantenimientoService.listarPorEquipo(id)).isEmpty();
    }
}
