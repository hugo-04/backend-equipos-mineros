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

import com.mineria.equipos.application.port.in.RegistrarMantenimientoUseCase.Comando;
import com.mineria.equipos.equipo.service.EquipoService;
import com.mineria.equipos.application.port.out.MantenimientoQueryPort;
import com.mineria.equipos.application.port.out.MantenimientoRepositoryPort;
import com.mineria.equipos.application.view.MantenimientoView;
import com.mineria.equipos.shared.exception.EquipoNotFoundException;
import com.mineria.equipos.domain.model.Mantenimiento;
import com.mineria.equipos.testsupport.Datos;

@ExtendWith(MockitoExtension.class)
class RegistrarMantenimientoServiceTest {

    @Mock
    private EquipoService equipoService;
    @Mock
    private MantenimientoRepositoryPort mantenimientoRepository;
    @Mock
    private MantenimientoQueryPort mantenimientoQuery;

    private RegistrarMantenimientoService service;

    @BeforeEach
    void setUp() {
        Clock reloj = Clock.fixed(Datos.AHORA, ZoneOffset.UTC);
        service = new RegistrarMantenimientoService(equipoService, mantenimientoRepository, mantenimientoQuery, reloj);
    }

    @Test
    @DisplayName("guarda el historial con el horómetro actual y reinicia el ciclo del equipo")
    void registraYReiniciaElCiclo() {
        // Arrange
        Mantenimiento guardado = Mantenimiento.registrarPara(8L, new BigDecimal("310.5"), "Cambio de aceite", Datos.AHORA)
                .toBuilder().id(21L).build();
        MantenimientoView esperado = Datos.mantenimientoView(21L, 8L, "310.5");
        when(equipoService.reiniciarCiclo(8L)).thenReturn(new BigDecimal("310.5"));
        when(mantenimientoRepository.guardar(any(Mantenimiento.class))).thenReturn(guardado);
        when(mantenimientoQuery.buscarPorId(21L)).thenReturn(Optional.of(esperado));

        // Act
        MantenimientoView resultado = service.registrar(new Comando(8L, "Cambio de aceite"));

        // Assert: historial con el horómetro del momento
        assertThat(resultado).isSameAs(esperado);
        ArgumentCaptor<Mantenimiento> historial = ArgumentCaptor.forClass(Mantenimiento.class);
        verify(mantenimientoRepository).guardar(historial.capture());
        assertThat(historial.getValue().getHorometroAlRegistrar()).isEqualByComparingTo("310.5");
        assertThat(historial.getValue().getFecha()).isEqualTo(Datos.AHORA);

        // Assert: el módulo equipo reinició el ciclo de ese equipo
        verify(equipoService).reiniciarCiclo(8L);
    }

    @Test
    @DisplayName("falla con EquipoNotFoundException si el equipo no existe y no guarda nada")
    void equipoInexistente() {
        // Arrange
        when(equipoService.reiniciarCiclo(8L)).thenThrow(new EquipoNotFoundException(8L));

        // Act + Assert
        assertThatThrownBy(() -> service.registrar(new Comando(8L, null)))
                .isInstanceOf(EquipoNotFoundException.class);
        verify(mantenimientoRepository, never()).guardar(any());
    }

    @Test
    @DisplayName("si falla guardar el historial, el error se propaga (la transacción deshace el reinicio)")
    void fallaAlGuardarElHistorial() {
        // Arrange
        when(equipoService.reiniciarCiclo(8L)).thenReturn(new BigDecimal("310.5"));
        when(mantenimientoRepository.guardar(any())).thenThrow(new IllegalStateException("BD caída"));

        // Act + Assert
        assertThatThrownBy(() -> service.registrar(new Comando(8L, null)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("falla si el mantenimiento recién guardado no se puede leer")
    void noSeEncuentraElMantenimientoGuardado() {
        // Arrange
        Mantenimiento guardado = Mantenimiento.registrarPara(8L, new BigDecimal("310.5"), null, Datos.AHORA)
                .toBuilder().id(21L).build();
        when(equipoService.reiniciarCiclo(8L)).thenReturn(new BigDecimal("310.5"));
        when(mantenimientoRepository.guardar(any())).thenReturn(guardado);
        when(mantenimientoQuery.buscarPorId(21L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> service.registrar(new Comando(8L, null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("21");
    }
}
