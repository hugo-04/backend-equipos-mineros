package com.mineria.equipos.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mineria.equipos.application.port.in.CrearEquipoUseCase.Comando;
import com.mineria.equipos.application.port.out.EquipoRepositoryPort;
import com.mineria.equipos.application.port.out.TipoEquipoRepositoryPort;
import com.mineria.equipos.application.view.EquipoView;
import com.mineria.equipos.domain.exception.CodigoDuplicadoException;
import com.mineria.equipos.domain.exception.HorometroInvalidoException;
import com.mineria.equipos.domain.exception.TipoEquipoNotFoundException;
import com.mineria.equipos.domain.model.Equipo;
import com.mineria.equipos.testsupport.Datos;

@ExtendWith(MockitoExtension.class)
class CrearEquipoServiceTest {

    @Mock
    private EquipoRepositoryPort equipoRepository;
    @Mock
    private TipoEquipoRepositoryPort tipoEquipoRepository;
    @Mock
    private EquipoViewAssembler assembler;

    private CrearEquipoService service;

    @BeforeEach
    void setUp() {
        Clock reloj = Clock.fixed(Datos.AHORA, ZoneOffset.UTC);
        service = new CrearEquipoService(equipoRepository, tipoEquipoRepository, assembler, reloj);
    }

    @Test
    @DisplayName("crea el equipo con el ciclo en 0 y devuelve su view")
    void creaElEquipo() {
        // Arrange
        EquipoView esperado = Datos.equipoView(5L, "CAM-005", "120.5", "0", 250);
        when(tipoEquipoRepository.buscarPorId(1L)).thenReturn(Optional.of(Datos.camion()));
        when(equipoRepository.existePorCodigo("CAM-005")).thenReturn(false);
        when(equipoRepository.guardar(any(Equipo.class))).thenReturn(Datos.equipo(5L, "CAM-005", "120.5"));
        when(assembler.porId(5L)).thenReturn(esperado);

        // Act
        EquipoView resultado = service.crear(new Comando("CAM-005", 1L, new BigDecimal("120.5")));

        // Assert
        assertThat(resultado).isSameAs(esperado);
        ArgumentCaptor<Equipo> guardado = ArgumentCaptor.forClass(Equipo.class);
        verify(equipoRepository).guardar(guardado.capture());
        assertThat(guardado.getValue().getHorometroUltimoMant()).isEqualByComparingTo("0");
        assertThat(guardado.getValue().getCreadoEn()).isEqualTo(Datos.AHORA);
    }

    @Test
    @DisplayName("falla con TipoEquipoNotFoundException si el tipo no existe")
    void tipoInexistente() {
        // Arrange
        when(tipoEquipoRepository.buscarPorId(99L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> service.crear(new Comando("CAM-005", 99L, BigDecimal.TEN)))
                .isInstanceOf(TipoEquipoNotFoundException.class);
        verify(equipoRepository, never()).guardar(any());
    }

    @Test
    @DisplayName("falla con CodigoDuplicadoException si el código ya existe")
    void codigoDuplicado() {
        // Arrange
        when(tipoEquipoRepository.buscarPorId(1L)).thenReturn(Optional.of(Datos.camion()));
        when(equipoRepository.existePorCodigo("CAM-001")).thenReturn(true);

        // Act + Assert
        assertThatThrownBy(() -> service.crear(new Comando("CAM-001", 1L, BigDecimal.TEN)))
                .isInstanceOf(CodigoDuplicadoException.class)
                .hasMessageContaining("CAM-001");
        verify(equipoRepository, never()).guardar(any());
    }

    @Test
    @DisplayName("propaga HorometroInvalidoException si el horómetro es negativo")
    void horometroNegativo() {
        // Arrange
        when(tipoEquipoRepository.buscarPorId(1L)).thenReturn(Optional.of(Datos.camion()));
        when(equipoRepository.existePorCodigo("CAM-005")).thenReturn(false);

        // Act + Assert
        assertThatThrownBy(() -> service.crear(new Comando("CAM-005", 1L, new BigDecimal("-1"))))
                .isInstanceOf(HorometroInvalidoException.class);
    }

    @Test
    @DisplayName("relanza los errores inesperados sin tragárselos")
    void errorInesperado() {
        // Arrange
        when(tipoEquipoRepository.buscarPorId(1L)).thenThrow(new IllegalStateException("BD caída"));

        // Act + Assert
        assertThatThrownBy(() -> service.crear(new Comando("CAM-005", 1L, BigDecimal.TEN)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("BD caída");
    }
}
