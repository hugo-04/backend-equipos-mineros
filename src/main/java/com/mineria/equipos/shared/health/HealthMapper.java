package com.mineria.equipos.shared.health;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.mineria.equipos.generated.model.HealthResponse;

/** Arma la respuesta del /health con dos datos: si la BD responde y qué versión está corriendo. */
@Mapper(imports = HealthResponse.class)
public interface HealthMapper {

    @Mapping(target = "status",
            expression = "java(disponible ? HealthResponse.StatusEnum.UP : HealthResponse.StatusEnum.DOWN)")
    @Mapping(target = "version", source = "version")
    HealthResponse toResponse(boolean disponible, String version);
}
