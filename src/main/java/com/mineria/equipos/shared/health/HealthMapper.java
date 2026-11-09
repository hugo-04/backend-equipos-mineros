package com.mineria.equipos.shared.health;

import java.time.Instant;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.mineria.equipos.generated.model.HealthResponse;

/** Arma la respuesta del /health con tres datos: si la BD responde, qué versión está corriendo y desde cuándo. */
@Mapper(imports = HealthResponse.class)
public interface HealthMapper {

    @Mapping(target = "status",
            expression = "java(disponible ? HealthResponse.StatusEnum.UP : HealthResponse.StatusEnum.DOWN)")
    @Mapping(target = "version", source = "version")
    @Mapping(target = "iniciadoEn", source = "iniciadoEn")
    HealthResponse toResponse(boolean disponible, String version, Instant iniciadoEn);
}
