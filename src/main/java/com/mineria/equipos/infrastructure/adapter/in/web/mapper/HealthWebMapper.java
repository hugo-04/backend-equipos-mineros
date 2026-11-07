package com.mineria.equipos.infrastructure.adapter.in.web.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.mineria.equipos.application.view.EstadoSalud;
import com.mineria.equipos.generated.model.HealthResponse;

/** Arma la respuesta de /health a partir del estado y la versión que corre. */
@Mapper
public interface HealthWebMapper {

    @Mapping(target = "status", source = "estado")
    @Mapping(target = "version", source = "version")
    HealthResponse toResponse(EstadoSalud estado, String version);
}
