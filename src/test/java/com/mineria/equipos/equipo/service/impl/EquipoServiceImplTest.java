package com.mineria.equipos.equipo.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import com.mineria.equipos.generated.model.PaginaEquipoResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.PageImpl;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.sql.SQLException;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import com.mineria.equipos.equipo.entity.Equipo;
import com.mineria.equipos.equipo.mapper.EquipoMapperImpl;
import com.mineria.equipos.equipo.policy.IntervaloMantenimientoPolicy;
import com.mineria.equipos.equipo.repository.EquipoRepository;
import com.mineria.equipos.generated.model.EquipoRequest;
import com.mineria.equipos.generated.model.EquipoResponse;
import com.mineria.equipos.shared.exception.CodigoDuplicadoException;
import com.mineria.equipos.shared.exception.EquipoNotFoundException;
import com.mineria.equipos.shared.exception.HorometroInvalidoException;
import com.mineria.equipos.shared.exception.TipoEquipoNotFoundException;
import com.mineria.equipos.testsupport.Datos;
import com.mineria.equipos.tipo.service.TipoService;

/**
 * Las reglas de negocio del equipo: horómetro que no baja, código único, ciclo de mantenimiento en 0 y los errores
 * esperados. El repositorio va simulado; el mapper y la política son los reales.
 */
@ExtendWith(MockitoExtension.class)
class EquipoServiceImplTest {

    @Mock
    private EquipoRepository repository;
    @Mock
    private TipoService tipoService;

    private EquipoServiceImpl service;

    @BeforeEach
    void setUp() {
        // El mapper y la política son los reales: así se prueba que el estado sale de la regla de mantenimiento
        EquipoMapperImpl mapper = new EquipoMapperImpl();
        ReflectionTestUtils.setField(mapper, "mantenimientoPolicy", new IntervaloMantenimientoPolicy());
        Clock reloj = Clock.fixed(Datos.AHORA, ZoneOffset.UTC);
        service = new EquipoServiceImpl(repository, mapper, tipoService, reloj);
    }

    private EquipoRequest request(String codigo, long tipoId, String horometro) {
        return new EquipoRequest(codigo, tipoId, new BigDecimal(horometro));
    }

    // crear

    @Test
    @DisplayName("crear: guarda el equipo con el ciclo en 0 y devuelve su DTO")
    void creaElEquipo() {
        // Arrange
        when(repository.existsByCodigo("CAM-005")).thenReturn(false);
        when(repository.saveAndFlush(any(Equipo.class))).thenReturn(Datos.equipo(5L, "CAM-005", "120.5"));
        when(repository.buscarProyeccionPorId(5L))
                .thenReturn(Optional.of(Datos.equipoProjection(5L, "CAM-005", "120.5", "0", 250)));

        // Act
        EquipoResponse resultado = service.crear(request("CAM-005", 1L, "120.5"));

        // Assert
        assertThat(resultado.getId()).isEqualTo(5L);
        assertThat(resultado.getRequiereMantenimiento()).isFalse();
        ArgumentCaptor<Equipo> guardado = ArgumentCaptor.forClass(Equipo.class);
        verify(repository).saveAndFlush(guardado.capture());
        assertThat(guardado.getValue().getHorometroUltimoMant()).isEqualByComparingTo("0");
        assertThat(guardado.getValue().getCreadoEn()).isEqualTo(Datos.AHORA);
        assertThat(guardado.getValue().getTipoId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("crear: el ciclo arranca en 0 aunque la entidad llegue con otro valor y se guarda la fecha")
    void crearArrancaElCicloEnCero() {
        // Arrange
        when(repository.existsByCodigo("CAM-006")).thenReturn(false);
        when(repository.saveAndFlush(any(Equipo.class))).thenAnswer(inv -> {
            Equipo e = inv.getArgument(0);
            e.setId(6L);
            return e;
        });
        when(repository.buscarProyeccionPorId(6L))
                .thenReturn(Optional.of(Datos.equipoProjection(6L, "CAM-006", "80.5", "0", 250)));

        // Act
        service.crear(request("CAM-006", 2L, "80.5"));

        // Assert
        ArgumentCaptor<Equipo> guardado = ArgumentCaptor.forClass(Equipo.class);
        verify(repository).saveAndFlush(guardado.capture());
        assertThat(guardado.getValue().getCodigo()).isEqualTo("CAM-006");
        assertThat(guardado.getValue().getTipoId()).isEqualTo(2L);
        assertThat(guardado.getValue().getHorometro()).isEqualByComparingTo("80.5");
        assertThat(guardado.getValue().getHorometroUltimoMant()).isEqualByComparingTo("0");
        assertThat(guardado.getValue().getCreadoEn()).isEqualTo(Datos.AHORA);
    }

    @Test
    @DisplayName("crear: falla con TipoEquipoNotFoundException si el tipo no existe")
    void crearTipoInexistente() {
        // Arrange
        doThrow(new TipoEquipoNotFoundException(99L)).when(tipoService).verificarExiste(99L);

        // Act + Assert
        assertThatThrownBy(() -> service.crear(request("CAM-005", 99L, "10")))
                .isInstanceOf(TipoEquipoNotFoundException.class);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("crear: falla con CodigoDuplicadoException si el código ya existe")
    void crearCodigoDuplicado() {
        // Arrange
        when(repository.existsByCodigo("CAM-001")).thenReturn(true);

        // Act + Assert
        assertThatThrownBy(() -> service.crear(request("CAM-001", 1L, "10")))
                .isInstanceOf(CodigoDuplicadoException.class)
                .hasMessageContaining("CAM-001");
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("crear: propaga HorometroInvalidoException si el horómetro es negativo")
    void crearHorometroNegativo() {
        // Arrange
        when(repository.existsByCodigo("CAM-005")).thenReturn(false);

        // Act + Assert
        assertThatThrownBy(() -> service.crear(request("CAM-005", 1L, "-1")))
                .isInstanceOf(HorometroInvalidoException.class);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("crear: un horómetro nulo también es inválido")
    void crearHorometroNulo() {
        // Arrange
        when(repository.existsByCodigo("CAM-005")).thenReturn(false);

        // Act + Assert
        assertThatThrownBy(() -> service.crear(new EquipoRequest("CAM-005", 1L, null)))
                .isInstanceOf(HorometroInvalidoException.class)
                .hasMessageContaining("mayor o igual a 0");
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("crear: dos altas simultáneas con el mismo código -> CodigoDuplicadoException")
    void crearCarreraPorCodigoUnico() {
        // Arrange: el chequeo previo pasa pero la restricción única de la BD lo frena
        when(repository.existsByCodigo("CAM-005")).thenReturn(false);
        when(repository.saveAndFlush(any(Equipo.class))).thenThrow(new DataIntegrityViolationException("x",
                new SQLException("Duplicate entry 'CAM-005' for key 'equipo.uk_equipo_codigo'")));

        // Act + Assert
        assertThatThrownBy(() -> service.crear(request("CAM-005", 1L, "10")))
                .isInstanceOf(CodigoDuplicadoException.class);
    }

    @Test
    @DisplayName("crear: otras violaciones de integridad se relanzan tal cual")
    void crearOtraViolacionDeIntegridad() {
        // Arrange
        when(repository.existsByCodigo("CAM-005")).thenReturn(false);
        when(repository.saveAndFlush(any(Equipo.class))).thenThrow(new DataIntegrityViolationException("x",
                new SQLException("Cannot add or update a child row: a foreign key constraint fails (fk_equipo_tipo)")));

        // Act + Assert
        assertThatThrownBy(() -> service.crear(request("CAM-005", 1L, "10")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("crear: una violación de integridad sin mensaje también se relanza")
    void crearViolacionSinMensaje() {
        // Arrange
        when(repository.existsByCodigo("CAM-005")).thenReturn(false);
        when(repository.saveAndFlush(any(Equipo.class))).thenThrow(new DataIntegrityViolationException(null));

        // Act + Assert
        assertThatThrownBy(() -> service.crear(request("CAM-005", 1L, "10")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("crear: relanza los errores inesperados sin tragárselos")
    void crearErrorInesperado() {
        // Arrange
        doThrow(new IllegalStateException("BD caída")).when(tipoService).verificarExiste(1L);

        // Act + Assert
        assertThatThrownBy(() -> service.crear(request("CAM-005", 1L, "10")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("BD caída");
    }

    // actualizar

    @Test
    @DisplayName("actualizar: actualiza el equipo cuando el horómetro sube")
    void actualizaCuandoSube() {
        // Arrange
        Equipo existente = Datos.equipo(3L, "CAM-003", "100.0");
        when(repository.findById(3L)).thenReturn(Optional.of(existente));
        when(repository.existsByCodigoAndIdNot("CAM-003", 3L)).thenReturn(false);
        when(repository.buscarProyeccionPorId(3L))
                .thenReturn(Optional.of(Datos.equipoProjection(3L, "CAM-003", "200.0", "0", 250)));

        // Act
        EquipoResponse resultado = service.actualizar(3L, request("CAM-003", 1L, "200.0"));

        // Assert
        assertThat(resultado.getHorometro()).isEqualByComparingTo("200.0");
        verify(repository).saveAndFlush(existente);
        assertThat(existente.getHorometro()).isEqualByComparingTo("200.0");
    }

    @Test
    @DisplayName("actualizar: cambia código y tipo sin tocar el ciclo de mantenimiento ni la fecha de creación")
    void actualizaCodigoYTipoSinTocarElCiclo() {
        // Arrange
        Equipo existente = Datos.equipo(3L, "CAM-003", "100.0");
        existente.setHorometroUltimoMant(new BigDecimal("40.0"));
        when(repository.findById(3L)).thenReturn(Optional.of(existente));
        when(repository.buscarProyeccionPorId(3L))
                .thenReturn(Optional.of(Datos.equipoProjection(3L, "EXC-009", "150.0", "40.0", 500)));

        // Act
        service.actualizar(3L, request("EXC-009", 2L, "150.0"));

        // Assert
        assertThat(existente.getCodigo()).isEqualTo("EXC-009");
        assertThat(existente.getTipoId()).isEqualTo(2L);
        assertThat(existente.getHorometro()).isEqualByComparingTo("150.0");
        assertThat(existente.getHorometroUltimoMant()).isEqualByComparingTo("40.0");
        assertThat(existente.getCreadoEn()).isEqualTo(Datos.AHORA);
        assertThat(existente.getId()).isEqualTo(3L);
    }

    @Test
    @DisplayName("actualizar: permite dejar el mismo horómetro")
    void actualizarPermiteIgual() {
        // Arrange
        Equipo existente = Datos.equipo(3L, "CAM-003", "100.0");
        when(repository.findById(3L)).thenReturn(Optional.of(existente));
        when(repository.buscarProyeccionPorId(3L))
                .thenReturn(Optional.of(Datos.equipoProjection(3L, "CAM-003", "100.0", "0", 250)));

        // Act + Assert
        assertThatCode(() -> service.actualizar(3L, request("CAM-003", 1L, "100.0"))).doesNotThrowAnyException();
        verify(repository).saveAndFlush(existente);
    }

    @Test
    @DisplayName("actualizar: rechaza un horómetro negativo")
    void actualizarRechazaNegativo() {
        // Arrange
        Equipo existente = Datos.equipo(3L, "CAM-003", "0.0");
        when(repository.findById(3L)).thenReturn(Optional.of(existente));

        // Act + Assert
        assertThatThrownBy(() -> service.actualizar(3L, request("CAM-003", 1L, "-1")))
                .isInstanceOf(HorometroInvalidoException.class)
                .hasMessageContaining("mayor o igual a 0");
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("actualizar: falla con EquipoNotFoundException si el equipo no existe")
    void actualizarEquipoInexistente() {
        // Arrange
        when(repository.findById(9L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> service.actualizar(9L, request("CAM-009", 1L, "10")))
                .isInstanceOf(EquipoNotFoundException.class);
    }

    @Test
    @DisplayName("actualizar: falla con TipoEquipoNotFoundException si el tipo no existe")
    void actualizarTipoInexistente() {
        // Arrange
        when(repository.findById(3L)).thenReturn(Optional.of(Datos.equipo(3L, "CAM-003", "100.0")));
        doThrow(new TipoEquipoNotFoundException(77L)).when(tipoService).verificarExiste(77L);

        // Act + Assert
        assertThatThrownBy(() -> service.actualizar(3L, request("CAM-003", 77L, "10")))
                .isInstanceOf(TipoEquipoNotFoundException.class);
    }

    @Test
    @DisplayName("actualizar: falla con HorometroInvalidoException si se intenta bajar el horómetro")
    void noPermiteBajarElHorometro() {
        // Arrange
        Equipo existente = Datos.equipo(3L, "CAM-003", "100.0");
        when(repository.findById(3L)).thenReturn(Optional.of(existente));

        // Act + Assert
        assertThatThrownBy(() -> service.actualizar(3L, request("CAM-003", 1L, "99.9")))
                .isInstanceOf(HorometroInvalidoException.class)
                .hasMessageContaining("no puede bajar");
        verify(repository, never()).saveAndFlush(any());
        assertThat(existente.getHorometro()).isEqualByComparingTo("100.0");
    }

    @Test
    @DisplayName("actualizar: falla con CodigoDuplicadoException si el código es de otro equipo")
    void actualizarCodigoDeOtroEquipo() {
        // Arrange
        Equipo existente = Datos.equipo(3L, "CAM-003", "100.0");
        when(repository.findById(3L)).thenReturn(Optional.of(existente));
        when(repository.existsByCodigoAndIdNot("CAM-001", 3L)).thenReturn(true);

        // Act + Assert
        assertThatThrownBy(() -> service.actualizar(3L, request("CAM-001", 1L, "100.0")))
                .isInstanceOf(CodigoDuplicadoException.class);
        verify(repository, never()).saveAndFlush(any());
        assertThat(existente.getCodigo()).isEqualTo("CAM-003");
    }

    @Test
    @DisplayName("actualizar: relanza los errores inesperados sin tragárselos")
    void actualizarErrorInesperado() {
        // Arrange
        when(repository.findById(3L)).thenThrow(new IllegalStateException("BD caída"));

        // Act + Assert
        assertThatThrownBy(() -> service.actualizar(3L, request("CAM-003", 1L, "10")))
                .isInstanceOf(IllegalStateException.class);
    }

    // eliminar

    @Test
    @DisplayName("eliminar: borra el equipo cuando existe")
    void eliminaCuandoExiste() {
        // Arrange
        when(repository.existsById(4L)).thenReturn(true);

        // Act
        service.eliminar(4L);

        // Assert
        verify(repository).deleteById(4L);
        verify(repository).flush();
    }

    @Test
    @DisplayName("eliminar: falla con EquipoNotFoundException cuando no existe")
    void eliminarInexistente() {
        // Arrange
        when(repository.existsById(4L)).thenReturn(false);

        // Act + Assert
        assertThatThrownBy(() -> service.eliminar(4L)).isInstanceOf(EquipoNotFoundException.class);
        verify(repository, never()).deleteById(4L);
    }

    @Test
    @DisplayName("eliminar: relanza los errores inesperados sin tragárselos")
    void eliminarErrorInesperado() {
        // Arrange
        when(repository.existsById(4L)).thenReturn(true);
        doThrow(new IllegalStateException("BD caída")).when(repository).deleteById(4L);

        // Act + Assert
        assertThatThrownBy(() -> service.eliminar(4L)).isInstanceOf(IllegalStateException.class);
    }

    // consultas (el estado de mantenimiento se calcula al consultar)

    @Test
    @DisplayName("obtener: marca 'Requiere mantenimiento' cuando se alcanza el intervalo")
    void requiereMantenimiento() {
        // Arrange: 250 h trabajadas con intervalo de 250 -> justo en el límite
        when(repository.buscarProyeccionPorId(1L))
                .thenReturn(Optional.of(Datos.equipoProjection(1L, "CAM-001", "250.0", "0", 250)));

        // Act
        EquipoResponse respuesta = service.obtener(1L);

        // Assert
        assertThat(respuesta.getRequiereMantenimiento()).isTrue();
        assertThat(respuesta.getEstado()).isEqualTo("Requiere mantenimiento");
    }

    @Test
    @DisplayName("obtener: marca 'OK' cuando todavía no se alcanza el intervalo")
    void estadoOk() {
        // Arrange
        when(repository.buscarProyeccionPorId(1L))
                .thenReturn(Optional.of(Datos.equipoProjection(1L, "CAM-001", "249.9", "0", 250)));

        // Act
        EquipoResponse respuesta = service.obtener(1L);

        // Assert
        assertThat(respuesta.getRequiereMantenimiento()).isFalse();
        assertThat(respuesta.getEstado()).isEqualTo("OK");
    }

    @Test
    @DisplayName("obtener: falla con EquipoNotFoundException si no existe")
    void obtenerInexistente() {
        // Arrange
        when(repository.buscarProyeccionPorId(5L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> service.obtener(5L)).isInstanceOf(EquipoNotFoundException.class);
    }

    @Test
    @DisplayName("listar: calcula el estado de cada equipo por separado")
    void listar() {
        // Arrange
        Pageable pagina = PageRequest.of(0, 20);
        when(repository.listarProyecciones(pagina)).thenReturn(new PageImpl<>(List.of(
                Datos.equipoProjection(1L, "CAM-001", "300.0", "0", 250),
                Datos.equipoProjection(2L, "CAM-002", "300.0", "100.0", 250)), pagina, 2));

        // Act
        PaginaEquipoResponse respuesta = service.listar(pagina);

        // Assert: el primero lleva 300 h desde el último mant., el segundo solo 200
        assertThat(respuesta.getContent()).extracting(EquipoResponse::getRequiereMantenimiento).containsExactly(true, false);
        assertThat(respuesta.getTotalElements()).isEqualTo(2L);
    }

    @Test
    @DisplayName("listar: copia los datos de paginación de Spring Data (la página 1 de 3 elementos con tamaño 2)")
    void listarMapeaLaPaginacion() {
        // Arrange: Spring Data numera desde 0; la 2.ª página (índice 1) tiene 1 elemento y es la última
        Pageable pagina = PageRequest.of(1, 2);
        when(repository.listarProyecciones(pagina)).thenReturn(new PageImpl<>(
                List.of(Datos.equipoProjection(3L, "CAM-003", "10.0", "0", 250)), pagina, 3));

        // Act
        PaginaEquipoResponse respuesta = service.listar(pagina);

        // Assert
        assertThat(respuesta.getPage()).isEqualTo(1);
        assertThat(respuesta.getSize()).isEqualTo(2);
        assertThat(respuesta.getTotalElements()).isEqualTo(3L);
        assertThat(respuesta.getTotalPages()).isEqualTo(2);
        assertThat(respuesta.getFirst()).isFalse();
        assertThat(respuesta.getLast()).isTrue();
    }

    // servicios para otros módulos

    @Test
    @DisplayName("reiniciarCiclo: deja el último mantenimiento en el horómetro actual y lo devuelve")
    void reiniciaElCiclo() {
        // Arrange
        Equipo existente = Datos.equipo(8L, "CAM-008", "310.5");
        when(repository.findById(8L)).thenReturn(Optional.of(existente));

        // Act
        BigDecimal horometro = service.reiniciarCiclo(8L);

        // Assert
        assertThat(horometro).isEqualByComparingTo("310.5");
        assertThat(existente.getHorometroUltimoMant()).isEqualByComparingTo("310.5");
        verify(repository).saveAndFlush(existente);
    }

    @Test
    @DisplayName("reiniciarCiclo: EquipoNotFoundException si el equipo no existe")
    void reiniciarCicloInexistente() {
        // Arrange
        when(repository.findById(8L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> service.reiniciarCiclo(8L)).isInstanceOf(EquipoNotFoundException.class);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("verificarExiste: no lanza si existe y lanza EquipoNotFoundException si no")
    void verificarExiste() {
        // Arrange
        when(repository.existsById(1L)).thenReturn(true);
        when(repository.existsById(2L)).thenReturn(false);

        // Act + Assert
        assertThatCode(() -> service.verificarExiste(1L)).doesNotThrowAnyException();
        assertThatThrownBy(() -> service.verificarExiste(2L)).isInstanceOf(EquipoNotFoundException.class);
    }
}
