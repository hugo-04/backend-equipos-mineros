package com.mineria.equipos.mantenimiento.mapper;

import java.util.List;

import org.mapstruct.Mapper;

import com.mineria.equipos.generated.model.MantenimientoResponse;
import com.mineria.equipos.mantenimiento.repository.projection.MantenimientoProjection;

/** Convierte la proyección del historial al DTO del contrato. */
@Mapper
public interface MantenimientoMapper {

    MantenimientoResponse toResponse(MantenimientoProjection projection);

    List<MantenimientoResponse> toResponses(List<MantenimientoProjection> projections);
}
