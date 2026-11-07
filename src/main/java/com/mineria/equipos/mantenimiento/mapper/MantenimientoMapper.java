package com.mineria.equipos.mantenimiento.mapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.data.domain.Page;

import com.mineria.equipos.generated.model.MantenimientoResponse;
import com.mineria.equipos.generated.model.PaginaMantenimientoResponse;
import com.mineria.equipos.mantenimiento.entity.Mantenimiento;
import com.mineria.equipos.mantenimiento.repository.projection.MantenimientoProjection;
import com.mineria.equipos.shared.pagination.MapeoDePagina;

/** Pasa la proyección del historial al DTO de la API y arma la entidad cuando se registra uno nuevo. */
@Mapper
public interface MantenimientoMapper {

    MantenimientoResponse toResponse(MantenimientoProjection projection);

    List<MantenimientoResponse> toResponses(List<MantenimientoProjection> projections);

    @MapeoDePagina
    PaginaMantenimientoResponse toPagina(Page<MantenimientoProjection> pagina);

    /** El horómetro que se guarda es el que tenía el equipo en el momento de registrar. */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "horometroAlRegistrar", source = "horometro")
    Mantenimiento toEntity(Long equipoId, BigDecimal horometro, String notas, Instant fecha);
}
