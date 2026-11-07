package com.mineria.equipos.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mineria.equipos.equipo.service.EquipoService;
import com.mineria.equipos.application.port.out.MantenimientoQueryPort;
import com.mineria.equipos.application.view.MantenimientoView;
import com.mineria.equipos.shared.exception.EquipoNotFoundException;
import com.mineria.equipos.testsupport.Datos;

/** Pruebas del caso de uso de solo lectura del historial de mantenimientos. */
@ExtendWith(MockitoExtension.class)
class ConsultasServiceTest {

    @Mock
    private EquipoService equipoService;
    @Mock
    private MantenimientoQueryPort mantenimientoQuery;

    @Test
    @DisplayName("ConsultarMantenimientos: devuelve el historial de un equipo que existe")
    void historialDeEquipoExistente() {
        // Arrange
        List<MantenimientoView> historial = List.of(Datos.mantenimientoView(1L, 4L, "300.0"));
        when(mantenimientoQuery.listarPorEquipo(4L)).thenReturn(historial);

        // Act
        List<MantenimientoView> resultado =
                new ConsultarMantenimientosService(equipoService, mantenimientoQuery).listarPorEquipo(4L);

        // Assert
        assertThat(resultado).isEqualTo(historial);
    }

    @Test
    @DisplayName("ConsultarMantenimientos: 404 (no lista vacía) si el equipo no existe")
    void historialDeEquipoInexistente() {
        // Arrange
        doThrow(new EquipoNotFoundException(4L)).when(equipoService).verificarExiste(4L);
        ConsultarMantenimientosService service = new ConsultarMantenimientosService(equipoService, mantenimientoQuery);

        // Act + Assert
        assertThatThrownBy(() -> service.listarPorEquipo(4L)).isInstanceOf(EquipoNotFoundException.class);
        verify(mantenimientoQuery, never()).listarPorEquipo(4L);
    }
}
