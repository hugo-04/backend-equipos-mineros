package com.mineria.equipos.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.mineria.equipos.application.port.out.EquipoRepositoryPort;
import com.mineria.equipos.application.port.out.MantenimientoQueryPort;
import com.mineria.equipos.application.port.out.MantenimientoRepositoryPort;
import com.mineria.equipos.application.view.MantenimientoView;
import com.mineria.equipos.shared.exception.EquipoNotFoundException;
import com.mineria.equipos.domain.model.Equipo;
import com.mineria.equipos.domain.model.Mantenimiento;
import com.mineria.equipos.testsupport.Datos;

@ExtendWith(MockitoExtension.class)
class RegistrarMantenimientoServiceTest {

    @Mock
    private EquipoRepositoryPort equipoRepository;
    @Mock
    private MantenimientoRepositoryPort mantenimientoRepository;
    @Mock
    private MantenimientoQueryPort mantenimientoQuery;

    private RegistrarMantenimientoService service;

    @BeforeEach
    void setUp() {
        Clock reloj = Clock.fixed(Datos.AHORA, ZoneOffset.UTC);
        service = new RegistrarMantenimientoService(equipoRepository, mantenimientoRepository, mantenimientoQuery, reloj);
    }

    @Test
    @DisplayName("guarda el historial con el horómetro actual y reinicia el ciclo del equipo")
    void registraYReiniciaElCiclo() {
        // Arrange
        Equipo equipo = Datos.equipo(8L, "CAM-008", "310.5");
        Mantenimiento guardado = Mantenimiento.registrarPara(equipo, "Cambio de aceite", Datos.AHORA)
                .toBuilder().id(21L).build();
        MantenimientoView esperado = Datos.mantenimientoView(21L, 8L, "310.5");
        when(equipoRepository.buscarPorId(8L)).thenReturn(Optional.of(equipo));
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

        // Assert: el equipo queda con el ciclo reiniciado en su horómetro actual
        ArgumentCaptor<Equipo> equipoGuardado = ArgumentCaptor.forClass(Equipo.class);
        verify(equipoRepository).guardar(equipoGuardado.capture());
        assertThat(equipoGuardado.getValue().getHorometroUltimoMant()).isEqualByComparingTo("310.5");
    }

    @Test
    @DisplayName("falla con EquipoNotFoundException si el equipo no existe y no guarda nada")
    void equipoInexistente() {
        // Arrange
        when(equipoRepository.buscarPorId(8L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> service.registrar(new Comando(8L, null)))
                .isInstanceOf(EquipoNotFoundException.class);
        verify(mantenimientoRepository, never()).guardar(any());
        verify(equipoRepository, never()).guardar(any());
    }

    @Test
    @DisplayName("si falla guardar el historial, no se reinicia el ciclo del equipo")
    void fallaAlGuardarElHistorial() {
        // Arrange
        when(equipoRepository.buscarPorId(8L)).thenReturn(Optional.of(Datos.equipo(8L, "CAM-008", "310.5")));
        when(mantenimientoRepository.guardar(any())).thenThrow(new IllegalStateException("BD caída"));

        // Act + Assert
        assertThatThrownBy(() -> service.registrar(new Comando(8L, null)))
                .isInstanceOf(IllegalStateException.class);
        verify(equipoRepository, never()).guardar(any());
    }

    @Test
    @DisplayName("falla si el mantenimiento recién guardado no se puede leer")
    void noSeEncuentraElMantenimientoGuardado() {
        // Arrange
        Equipo equipo = Datos.equipo(8L, "CAM-008", "310.5");
        Mantenimiento guardado = Mantenimiento.registrarPara(equipo, null, Datos.AHORA).toBuilder().id(21L).build();
        when(equipoRepository.buscarPorId(8L)).thenReturn(Optional.of(equipo));
        when(mantenimientoRepository.guardar(any())).thenReturn(guardado);
        when(mantenimientoQuery.buscarPorId(21L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> service.registrar(new Comando(8L, null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("21");
    }
}
