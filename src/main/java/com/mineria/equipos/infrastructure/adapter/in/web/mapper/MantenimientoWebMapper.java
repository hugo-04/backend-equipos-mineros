package com.mineria.equipos.infrastructure.adapter.in.web.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.mineria.equipos.application.port.in.RegistrarMantenimientoUseCase;
import com.mineria.equipos.application.view.MantenimientoView;
import com.mineria.equipos.infrastructure.adapter.in.web.generated.model.MantenimientoRequest;
import com.mineria.equipos.infrastructure.adapter.in.web.generated.model.MantenimientoResponse;

/** Convierte entre los DTOs de mantenimientos y los comandos/views de la capa de aplicación. */
@Mapper
public interface MantenimientoWebMapper {

    @Mapping(target = "equipoId", source = "equipoId")
    RegistrarMantenimientoUseCase.Comando toComando(Long equipoId, MantenimientoRequest request);

    MantenimientoResponse toResponse(MantenimientoView view);

    List<MantenimientoResponse> toResponses(List<MantenimientoView> views);
}
