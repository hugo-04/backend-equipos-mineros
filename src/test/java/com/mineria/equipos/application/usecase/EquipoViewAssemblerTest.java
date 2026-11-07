package com.mineria.equipos.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mineria.equipos.application.port.out.EquipoQueryPort;
import com.mineria.equipos.application.view.EquipoView;
import com.mineria.equipos.domain.exception.EquipoNotFoundException;
import com.mineria.equipos.domain.rule.IntervaloMantenimientoPolicy;
import com.mineria.equipos.testsupport.Datos;

@ExtendWith(MockitoExtension.class)
class EquipoViewAssemblerTest {

    @Mock
    private EquipoQueryPort equipoQuery;

    private EquipoViewAssembler assembler;

    @BeforeEach
    void setUp() {
        // La política es la real: así se prueba que el estado sale de la regla del dominio
        assembler = new EquipoViewAssembler(equipoQuery, new IntervaloMantenimientoPolicy());
    }

    @Test
    @DisplayName("porId: marca 'Requiere mantenimiento' cuando se alcanza el intervalo")
    void requiereMantenimiento() {
        // Arrange: 250 h trabajadas con intervalo de 250 -> justo en el límite
        when(equipoQuery.buscarVistaPorId(1L)).thenReturn(Optional.of(Datos.equipoView(1L, "CAM-001", "250.0", "0", 250)));

        // Act
        EquipoView vista = assembler.porId(1L);

        // Assert
        assertThat(vista.isRequiereMantenimiento()).isTrue();
        assertThat(vista.getEstado()).isEqualTo("Requiere mantenimiento");
    }

    @Test
    @DisplayName("porId: marca 'OK' cuando todavía no se alcanza el intervalo")
    void estadoOk() {
        // Arrange
        when(equipoQuery.buscarVistaPorId(1L)).thenReturn(Optional.of(Datos.equipoView(1L, "CAM-001", "249.9", "0", 250)));

        // Act
        EquipoView vista = assembler.porId(1L);

        // Assert
        assertThat(vista.isRequiereMantenimiento()).isFalse();
        assertThat(vista.getEstado()).isEqualTo("OK");
    }

    @Test
    @DisplayName("porId: falla con EquipoNotFoundException si no existe")
    void noExiste() {
        // Arrange
        when(equipoQuery.buscarVistaPorId(5L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> assembler.porId(5L)).isInstanceOf(EquipoNotFoundException.class);
    }

    @Test
    @DisplayName("todos: calcula el estado de cada equipo por separado")
    void todos() {
        // Arrange
        when(equipoQuery.listar()).thenReturn(List.of(
                Datos.equipoView(1L, "CAM-001", "300.0", "0", 250),
                Datos.equipoView(2L, "CAM-002", "300.0", "100.0", 250)));

        // Act
        List<EquipoView> vistas = assembler.todos();

        // Assert: el primero lleva 300 h desde el último mant., el segundo solo 200
        assertThat(vistas).extracting(EquipoView::isRequiereMantenimiento).containsExactly(true, false);
    }
}
