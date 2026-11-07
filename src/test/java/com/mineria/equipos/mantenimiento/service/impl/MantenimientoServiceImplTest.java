package com.mineria.equipos.mantenimiento.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import com.mineria.equipos.generated.model.PaginaMantenimientoResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.PageImpl;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mineria.equipos.equipo.service.EquipoService;
import com.mineria.equipos.generated.model.MantenimientoRequest;
import com.mineria.equipos.generated.model.MantenimientoResponse;
import com.mineria.equipos.mantenimiento.entity.Mantenimiento;
import com.mineria.equipos.mantenimiento.mapper.MantenimientoMapperImpl;
import com.mineria.equipos.mantenimiento.repository.MantenimientoRepository;
import com.mineria.equipos.shared.exception.EquipoNotFoundException;
import com.mineria.equipos.testsupport.Datos;

@ExtendWith(MockitoExtension.class)
class MantenimientoServiceImplTest {

    @Mock
    private MantenimientoRepository repository;
    @Mock
    private EquipoService equipoService;

    private MantenimientoServiceImpl service;

    @BeforeEach
    void setUp() {
        Clock reloj = Clock.fixed(Datos.AHORA, ZoneOffset.UTC);
        service = new MantenimientoServiceImpl(repository, new MantenimientoMapperImpl(), equipoService, reloj);
    }

    private Mantenimiento guardadoConId(Long id) {
        Mantenimiento m = Datos.mantenimiento(8L, "310.5", "Cambio de aceite", Datos.AHORA);
        m.setId(id);
        return m;
    }

    // ---------- registrar ----------

    @Test
    @DisplayName("registrar: guarda el historial con el horómetro actual y reinicia el ciclo del equipo")
    void registraYReiniciaElCiclo() {
        // Arrange
        when(equipoService.reiniciarCiclo(8L)).thenReturn(new BigDecimal("310.5"));
        when(repository.saveAndFlush(any(Mantenimiento.class))).thenReturn(guardadoConId(21L));
        when(repository.findProjectedById(21L)).thenReturn(Optional.of(Datos.mantenimientoProjection(21L, 8L, "310.5")));

        // Act
        MantenimientoResponse resultado = service.registrar(8L, new MantenimientoRequest().notas("Cambio de aceite"));

        // Assert: respuesta
        assertThat(resultado.getId()).isEqualTo(21L);
        assertThat(resultado.getHorometroAlRegistrar()).isEqualByComparingTo("310.5");

        // Assert: historial con el horómetro del momento
        ArgumentCaptor<Mantenimiento> historial = ArgumentCaptor.forClass(Mantenimiento.class);
        verify(repository).saveAndFlush(historial.capture());
        assertThat(historial.getValue().getEquipoId()).isEqualTo(8L);
        assertThat(historial.getValue().getHorometroAlRegistrar()).isEqualByComparingTo("310.5");
        assertThat(historial.getValue().getNotas()).isEqualTo("Cambio de aceite");
        assertThat(historial.getValue().getFecha()).isEqualTo(Datos.AHORA);

        // Assert: el módulo equipo reinició el ciclo de ese equipo
        verify(equipoService).reiniciarCiclo(8L);
    }

    @Test
    @DisplayName("registrar: falla con EquipoNotFoundException si el equipo no existe y no guarda nada")
    void equipoInexistente() {
        // Arrange
        when(equipoService.reiniciarCiclo(8L)).thenThrow(new EquipoNotFoundException(8L));

        // Act + Assert
        assertThatThrownBy(() -> service.registrar(8L, new MantenimientoRequest()))
                .isInstanceOf(EquipoNotFoundException.class);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("registrar: si falla guardar el historial, el error se propaga (la transacción deshace el reinicio)")
    void fallaAlGuardarElHistorial() {
        // Arrange
        when(equipoService.reiniciarCiclo(8L)).thenReturn(new BigDecimal("310.5"));
        when(repository.saveAndFlush(any())).thenThrow(new IllegalStateException("BD caída"));

        // Act + Assert
        assertThatThrownBy(() -> service.registrar(8L, new MantenimientoRequest()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("BD caída");
    }

    @Test
    @DisplayName("registrar: falla si el mantenimiento recién guardado no se puede leer")
    void noSeEncuentraElMantenimientoGuardado() {
        // Arrange
        when(equipoService.reiniciarCiclo(8L)).thenReturn(new BigDecimal("310.5"));
        when(repository.saveAndFlush(any())).thenReturn(guardadoConId(21L));
        when(repository.findProjectedById(21L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> service.registrar(8L, new MantenimientoRequest()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("21");
    }

    // ---------- listarPorEquipo ----------

    @Test
    @DisplayName("listarPorEquipo: devuelve el historial de un equipo que existe")
    void historialDeEquipoExistente() {
        // Arrange
        Pageable pagina = PageRequest.of(0, 20);
        when(repository.findByEquipoIdOrderByFechaDescIdDesc(4L, pagina))
                .thenReturn(new PageImpl<>(List.of(Datos.mantenimientoProjection(1L, 4L, "300.0")), pagina, 1));

        // Act
        PaginaMantenimientoResponse resultado = service.listarPorEquipo(4L, pagina);

        // Assert
        verify(equipoService).verificarExiste(4L);
        assertThat(resultado.getContent()).singleElement().satisfies(m -> {
            assertThat(m.getEquipoId()).isEqualTo(4L);
            assertThat(m.getNotas()).isEqualTo("Cambio de aceite");
        });
        assertThat(resultado.getTotalElements()).isEqualTo(1L);
        assertThat(resultado.getPage()).isZero();
        assertThat(resultado.getLast()).isTrue();
    }

    @Test
    @DisplayName("listarPorEquipo: 404 (no lista vacía) si el equipo no existe")
    void historialDeEquipoInexistente() {
        // Arrange
        doThrow(new EquipoNotFoundException(4L)).when(equipoService).verificarExiste(4L);

        // Act + Assert
        Pageable pagina = PageRequest.of(0, 20);
        assertThatThrownBy(() -> service.listarPorEquipo(4L, pagina)).isInstanceOf(EquipoNotFoundException.class);
        verify(repository, never()).findByEquipoIdOrderByFechaDescIdDesc(4L, pagina);
    }
}
