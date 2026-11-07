package com.mineria.equipos.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mineria.equipos.application.port.in.ActualizarEquipoUseCase.Comando;
import com.mineria.equipos.application.port.out.EquipoRepositoryPort;
import com.mineria.equipos.application.port.out.TipoEquipoRepositoryPort;
import com.mineria.equipos.application.view.EquipoView;
import com.mineria.equipos.shared.exception.CodigoDuplicadoException;
import com.mineria.equipos.shared.exception.EquipoNotFoundException;
import com.mineria.equipos.shared.exception.HorometroInvalidoException;
import com.mineria.equipos.shared.exception.TipoEquipoNotFoundException;
import com.mineria.equipos.domain.model.Equipo;
import com.mineria.equipos.testsupport.Datos;

@ExtendWith(MockitoExtension.class)
class ActualizarEquipoServiceTest {

    @Mock
    private EquipoRepositoryPort equipoRepository;
    @Mock
    private TipoEquipoRepositoryPort tipoEquipoRepository;
    @Mock
    private EquipoViewAssembler assembler;

    private ActualizarEquipoService service;

    @BeforeEach
    void setUp() {
        service = new ActualizarEquipoService(equipoRepository, tipoEquipoRepository, assembler);
    }

    @Test
    @DisplayName("actualiza el equipo cuando el horómetro sube")
    void actualizaCuandoSube() {
        // Arrange
        EquipoView esperado = Datos.equipoView(3L, "CAM-003", "200.0", "0", 250);
        when(equipoRepository.buscarPorId(3L)).thenReturn(Optional.of(Datos.equipo(3L, "CAM-003", "100.0")));
        when(tipoEquipoRepository.buscarPorId(1L)).thenReturn(Optional.of(Datos.camion()));
        when(equipoRepository.existePorCodigoDistintoDe("CAM-003", 3L)).thenReturn(false);
        when(assembler.porId(3L)).thenReturn(esperado);

        // Act
        EquipoView resultado = service.actualizar(new Comando(3L, "CAM-003", 1L, new BigDecimal("200.0")));

        // Assert
        assertThat(resultado).isSameAs(esperado);
        ArgumentCaptor<Equipo> guardado = ArgumentCaptor.forClass(Equipo.class);
        verify(equipoRepository).guardar(guardado.capture());
        assertThat(guardado.getValue().getHorometro()).isEqualByComparingTo("200.0");
    }

    @Test
    @DisplayName("falla con EquipoNotFoundException si el equipo no existe")
    void equipoInexistente() {
        // Arrange
        when(equipoRepository.buscarPorId(9L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> service.actualizar(new Comando(9L, "CAM-009", 1L, BigDecimal.TEN)))
                .isInstanceOf(EquipoNotFoundException.class);
    }

    @Test
    @DisplayName("falla con TipoEquipoNotFoundException si el tipo no existe")
    void tipoInexistente() {
        // Arrange
        when(equipoRepository.buscarPorId(3L)).thenReturn(Optional.of(Datos.equipo(3L, "CAM-003", "100.0")));
        when(tipoEquipoRepository.buscarPorId(77L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> service.actualizar(new Comando(3L, "CAM-003", 77L, BigDecimal.TEN)))
                .isInstanceOf(TipoEquipoNotFoundException.class);
    }

    @Test
    @DisplayName("falla con HorometroInvalidoException si se intenta bajar el horómetro")
    void noPermiteBajarElHorometro() {
        // Arrange
        when(equipoRepository.buscarPorId(3L)).thenReturn(Optional.of(Datos.equipo(3L, "CAM-003", "100.0")));
        when(tipoEquipoRepository.buscarPorId(1L)).thenReturn(Optional.of(Datos.camion()));

        // Act + Assert
        assertThatThrownBy(() -> service.actualizar(new Comando(3L, "CAM-003", 1L, new BigDecimal("99.9"))))
                .isInstanceOf(HorometroInvalidoException.class);
        verify(equipoRepository, never()).guardar(any());
    }

    @Test
    @DisplayName("falla con CodigoDuplicadoException si el código es de otro equipo")
    void codigoDeOtroEquipo() {
        // Arrange
        when(equipoRepository.buscarPorId(3L)).thenReturn(Optional.of(Datos.equipo(3L, "CAM-003", "100.0")));
        when(tipoEquipoRepository.buscarPorId(1L)).thenReturn(Optional.of(Datos.camion()));
        when(equipoRepository.existePorCodigoDistintoDe("CAM-001", 3L)).thenReturn(true);

        // Act + Assert
        assertThatThrownBy(() -> service.actualizar(new Comando(3L, "CAM-001", 1L, new BigDecimal("100.0"))))
                .isInstanceOf(CodigoDuplicadoException.class);
        verify(equipoRepository, never()).guardar(any());
    }

    @Test
    @DisplayName("relanza los errores inesperados sin tragárselos")
    void errorInesperado() {
        // Arrange
        when(equipoRepository.buscarPorId(3L)).thenThrow(new IllegalStateException("BD caída"));

        // Act + Assert
        assertThatThrownBy(() -> service.actualizar(new Comando(3L, "CAM-003", 1L, BigDecimal.TEN)))
                .isInstanceOf(IllegalStateException.class);
    }
}
