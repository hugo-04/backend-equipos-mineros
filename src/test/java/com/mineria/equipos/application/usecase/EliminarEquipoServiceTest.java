package com.mineria.equipos.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mineria.equipos.application.port.out.EquipoRepositoryPort;
import com.mineria.equipos.shared.exception.EquipoNotFoundException;

@ExtendWith(MockitoExtension.class)
class EliminarEquipoServiceTest {

    @Mock
    private EquipoRepositoryPort equipoRepository;

    @InjectMocks
    private EliminarEquipoService service;

    @Test
    @DisplayName("elimina el equipo cuando existe")
    void eliminaCuandoExiste() {
        // Arrange
        when(equipoRepository.existePorId(4L)).thenReturn(true);

        // Act
        service.eliminar(4L);

        // Assert
        verify(equipoRepository).eliminarPorId(4L);
    }

    @Test
    @DisplayName("falla con EquipoNotFoundException cuando no existe")
    void noExiste() {
        // Arrange
        when(equipoRepository.existePorId(4L)).thenReturn(false);

        // Act + Assert
        assertThatThrownBy(() -> service.eliminar(4L)).isInstanceOf(EquipoNotFoundException.class);
        verify(equipoRepository, never()).eliminarPorId(4L);
    }

    @Test
    @DisplayName("relanza los errores inesperados sin tragárselos")
    void errorInesperado() {
        // Arrange
        when(equipoRepository.existePorId(4L)).thenReturn(true);
        doThrow(new IllegalStateException("BD caída")).when(equipoRepository).eliminarPorId(4L);

        // Act + Assert
        assertThatThrownBy(() -> service.eliminar(4L)).isInstanceOf(IllegalStateException.class);
    }
}
