package com.mineria.equipos.equipo.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import com.mineria.equipos.equipo.entity.Equipo;
import com.mineria.equipos.equipo.policy.IntervaloMantenimientoPolicy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import com.mineria.equipos.equipo.repository.projection.EquipoProjection;
import com.mineria.equipos.generated.model.PaginaEquipoResponse;
import com.mineria.equipos.generated.model.EquipoRequest;
import com.mineria.equipos.generated.model.EquipoResponse;
import com.mineria.equipos.generated.model.EquipoUpdateRequest;
import com.mineria.equipos.testsupport.Datos;

/**
 * El paso entre los DTOs, la entidad y la proyección, y que "requiereMantenimiento" y "estado" se calculen con la política.
 */
@SpringJUnitConfig({EquipoMapperImpl.class, IntervaloMantenimientoPolicy.class})
class EquipoMapperTest {

    @Autowired
    private EquipoMapper mapper;

    @Test
    @DisplayName("proyección -> DTO: la proyección aplanada se expone con el objeto 'tipo' anidado")
    void proyeccionAResponseConTipoAnidado() {
        // Arrange
        var proyeccion = Datos.equipoProjection(5L, "CAM-005", "260.0", "0", 250);

        // Act
        EquipoResponse response = mapper.toResponse(proyeccion);

        // Assert
        assertThat(response.getId()).isEqualTo(5L);
        assertThat(response.getTipo().getId()).isEqualTo(1L);
        assertThat(response.getTipo().getNombre()).isEqualTo("Camión");
        assertThat(response.getTipo().getIntervaloHoras()).isEqualTo(250);
        assertThat(response.getCreadoEn()).isEqualTo(Datos.AHORA);
    }

    @Test
    @DisplayName("proyección -> DTO: calcula requiereMantenimiento y el estado legible con la política")
    void calculaElEstadoDeMantenimiento() {
        // Arrange: 260 h de un intervalo de 250 y 200 h de 250
        var vencido = Datos.equipoProjection(1L, "CAM-001", "260.0", "0", 250);
        var vigente = Datos.equipoProjection(2L, "CAM-002", "300.0", "100.0", 250);

        // Act
        EquipoResponse r1 = mapper.toResponse(vencido);
        EquipoResponse r2 = mapper.toResponse(vigente);

        // Assert
        assertThat(r1.getRequiereMantenimiento()).isTrue();
        assertThat(r1.getEstado()).isEqualTo("Requiere mantenimiento");
        assertThat(r2.getRequiereMantenimiento()).isFalse();
        assertThat(r2.getEstado()).isEqualTo("OK");
    }

    @Test
    @DisplayName("proyección -> DTO: calcula horasDesdeUltimoMant y horasRestantes (nunca negativas)")
    void calculaLasHoras() {
        // Arrange: 100 h con el ciclo en 0, 410 h con el ciclo en 160 y 260 h (ya pasó el intervalo)
        var enCurso = Datos.equipoProjection(1L, "CAM-001", "100.0", "0", 250);
        var conCiclo = Datos.equipoProjection(2L, "CAM-002", "410.0", "160.0", 250);
        var vencido = Datos.equipoProjection(3L, "CAM-003", "260.0", "0", 250);

        // Act
        EquipoResponse r1 = mapper.toResponse(enCurso);
        EquipoResponse r2 = mapper.toResponse(conCiclo);
        EquipoResponse r3 = mapper.toResponse(vencido);

        // Assert
        assertThat(r1.getHorasDesdeUltimoMant()).isEqualByComparingTo("100.0");
        assertThat(r1.getHorasRestantes()).isEqualByComparingTo("150.0");
        assertThat(r2.getHorasDesdeUltimoMant()).isEqualByComparingTo("250.0");
        assertThat(r2.getHorasRestantes()).isEqualByComparingTo("0");
        assertThat(r3.getHorasDesdeUltimoMant()).isEqualByComparingTo("260.0");
        assertThat(r3.getHorasRestantes()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("convierte listas de proyecciones")
    void listas() {
        // Act
        List<EquipoResponse> respuestas = mapper.toResponses(List.of(
                Datos.equipoProjection(1L, "A-1", "1", "0", 250), Datos.equipoProjection(2L, "A-2", "2", "0", 250)));

        // Assert
        assertThat(respuestas).extracting(EquipoResponse::getCodigo).containsExactly("A-1", "A-2");
    }

    @Test
    @DisplayName("request -> entidad: copia código, tipo y horómetro y deja sin fijar id, ciclo y fecha (lo fija el service)")
    void requestAEntidad() {
        // Arrange
        EquipoRequest request = new EquipoRequest("CAM-001", 2L, new BigDecimal("12.5"));

        // Act
        Equipo entidad = mapper.toEntity(request);

        // Assert
        assertThat(entidad.getCodigo()).isEqualTo("CAM-001");
        assertThat(entidad.getTipoId()).isEqualTo(2L);
        assertThat(entidad.getHorometro()).isEqualByComparingTo("12.5");
        assertThat(entidad.getId()).isNull();
        assertThat(entidad.getCreadoEn()).isNull();
        assertThat(entidad.getHorometroUltimoMant()).isNull();
    }

    @Test
    @DisplayName("actualizar: copia código y horómetro sobre la entidad sin tocar id, tipo, ciclo ni fecha")
    void actualizaLaEntidadExistente() {
        // Arrange
        Equipo existente = Datos.equipo(3L, "CAM-003", "100.0");
        existente.setHorometroUltimoMant(new BigDecimal("40.0"));

        // Act
        mapper.actualizar(new EquipoUpdateRequest("CAM-009", new BigDecimal("150.0")), existente);

        // Assert
        assertThat(existente.getCodigo()).isEqualTo("CAM-009");
        assertThat(existente.getHorometro()).isEqualByComparingTo("150.0");
        assertThat(existente.getTipoId()).isEqualTo(1L);
        assertThat(existente.getId()).isEqualTo(3L);
        assertThat(existente.getHorometroUltimoMant()).isEqualByComparingTo("40.0");
        assertThat(existente.getCreadoEn()).isEqualTo(Datos.AHORA);
    }

    @Test
    @DisplayName("actualizar sin request no cambia la entidad")
    void actualizarSinRequest() {
        // Arrange
        Equipo existente = Datos.equipo(3L, "CAM-003", "100.0");

        // Act
        mapper.actualizar(null, existente);

        // Assert
        assertThat(existente.getCodigo()).isEqualTo("CAM-003");
    }

    @Test
    @DisplayName("Page de Spring Data -> página del contrato: copia contenido, número, tamaño, totales y bandera first/last")
    void paginaDelContrato() {
        // Arrange: página 1 (la segunda) de tamaño 2 con 5 elementos en total
        var pageable = PageRequest.of(1, 2);
        Page<EquipoProjection> pagina = new PageImpl<>(List.of(
                Datos.equipoProjection(3L, "A-3", "1", "0", 250), Datos.equipoProjection(4L, "A-4", "2", "0", 250)),
                pageable, 5);

        // Act
        PaginaEquipoResponse respuesta = mapper.toPagina(pagina);

        // Assert
        assertThat(respuesta.getContent()).extracting(EquipoResponse::getCodigo).containsExactly("A-3", "A-4");
        assertThat(respuesta.getPage()).isEqualTo(1);
        assertThat(respuesta.getSize()).isEqualTo(2);
        assertThat(respuesta.getTotalElements()).isEqualTo(5L);
        assertThat(respuesta.getTotalPages()).isEqualTo(3);
        assertThat(respuesta.getFirst()).isFalse();
        assertThat(respuesta.getLast()).isFalse();
        assertThat(mapper.toPagina(null)).isNull();
    }

    @Test
    @DisplayName("devuelve null (o lista nula) cuando recibe null")
    void nulos() {
        // Act + Assert
        assertThat(mapper.toResponse(null)).isNull();
        assertThat(mapper.toResponses(null)).isNull();
        assertThat(mapper.toEntity(null)).isNull();
    }
}
