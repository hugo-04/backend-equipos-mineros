package com.mineria.equipos.mantenimiento.mapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.mineria.equipos.generated.model.MantenimientoResponse;
import com.mineria.equipos.mantenimiento.entity.Mantenimiento;
import com.mineria.equipos.mantenimiento.repository.projection.MantenimientoProjection;

/** Convierte la proyección del historial al DTO del contrato y arma la entidad de un registro nuevo. */
@Mapper
public interface MantenimientoMapper {

    MantenimientoResponse toResponse(MantenimientoProjection projection);

    List<MantenimientoResponse> toResponses(List<MantenimientoProjection> projections);

    /** Arma el registro del historial con el horómetro que tenía el equipo al registrarlo. */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "horometroAlRegistrar", source = "horometro")
    Mantenimiento toEntity(Long equipoId, BigDecimal horometro, String notas, Instant fecha);
}
