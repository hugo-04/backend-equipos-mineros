package com.mineria.equipos.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mineria.equipos.application.port.out.EquipoRepositoryPort;
import com.mineria.equipos.application.port.out.MantenimientoQueryPort;
import com.mineria.equipos.application.port.out.TipoEquipoQueryPort;
import com.mineria.equipos.application.view.EquipoView;
import com.mineria.equipos.application.view.MantenimientoView;
import com.mineria.equipos.application.view.TipoEquipoView;
import com.mineria.equipos.domain.exception.EquipoNotFoundException;
import com.mineria.equipos.testsupport.Datos;

/** Pruebas de los casos de uso de solo lectura (equipos, tipos y mantenimientos). */
@ExtendWith(MockitoExtension.class)
class ConsultasServiceTest {

    @Mock
    private EquipoViewAssembler assembler;
    @Mock
    private TipoEquipoQueryPort tipoEquipoQuery;
    @Mock
    private EquipoRepositoryPort equipoRepository;
    @Mock
    private MantenimientoQueryPort mantenimientoQuery;

    @Test
    @DisplayName("ConsultarEquipos.listar: devuelve lo que arma el ensamblador")
    void listarEquipos() {
        // Arrange
        List<EquipoView> vistas = List.of(Datos.equipoView(1L, "CAM-001", "10", "0", 250));
        when(assembler.todos()).thenReturn(vistas);

        // Act
        List<EquipoView> resultado = new ConsultarEquiposService(assembler).listar();

        // Assert
        assertThat(resultado).isEqualTo(vistas);
    }

    @Test
    @DisplayName("ConsultarEquipos.obtener: devuelve el equipo pedido")
    void obtenerEquipo() {
        // Arrange
        EquipoView vista = Datos.equipoView(1L, "CAM-001", "10", "0", 250);
        when(assembler.porId(1L)).thenReturn(vista);

        // Act
        EquipoView resultado = new ConsultarEquiposService(assembler).obtener(1L);

        // Assert
        assertThat(resultado).isSameAs(vista);
    }

    @Test
    @DisplayName("ConsultarTipos.listar: devuelve el catálogo")
    void listarTipos() {
        // Arrange
        List<TipoEquipoView> tipos = List.of(TipoEquipoView.builder().id(1L).nombre("Camión").intervaloHoras(250).build());
        when(tipoEquipoQuery.listar()).thenReturn(tipos);

        // Act
        List<TipoEquipoView> resultado = new ConsultarTiposService(tipoEquipoQuery).listar();

        // Assert
        assertThat(resultado).isEqualTo(tipos);
    }

    @Test
    @DisplayName("ConsultarMantenimientos: devuelve el historial de un equipo que existe")
    void historialDeEquipoExistente() {
        // Arrange
        List<MantenimientoView> historial = List.of(Datos.mantenimientoView(1L, 4L, "300.0"));
        when(equipoRepository.existePorId(4L)).thenReturn(true);
        when(mantenimientoQuery.listarPorEquipo(4L)).thenReturn(historial);

        // Act
        List<MantenimientoView> resultado =
                new ConsultarMantenimientosService(equipoRepository, mantenimientoQuery).listarPorEquipo(4L);

        // Assert
        assertThat(resultado).isEqualTo(historial);
    }

    @Test
    @DisplayName("ConsultarMantenimientos: 404 (no lista vacía) si el equipo no existe")
    void historialDeEquipoInexistente() {
        // Arrange
        when(equipoRepository.existePorId(4L)).thenReturn(false);
        ConsultarMantenimientosService service = new ConsultarMantenimientosService(equipoRepository, mantenimientoQuery);

        // Act + Assert
        assertThatThrownBy(() -> service.listarPorEquipo(4L)).isInstanceOf(EquipoNotFoundException.class);
        verify(mantenimientoQuery, never()).listarPorEquipo(4L);
    }
}
