package com.mineria.equipos.tipo.mapper;

import java.util.List;

import org.mapstruct.Mapper;

import com.mineria.equipos.generated.model.TipoEquipoResponse;
import com.mineria.equipos.tipo.repository.projection.TipoProjection;

/** Convierte la proyección de un tipo de equipo al DTO del contrato. */
@Mapper
public interface TipoMapper {

    TipoEquipoResponse toResponse(TipoProjection projection);

    List<TipoEquipoResponse> toResponses(List<TipoProjection> projections);
}
