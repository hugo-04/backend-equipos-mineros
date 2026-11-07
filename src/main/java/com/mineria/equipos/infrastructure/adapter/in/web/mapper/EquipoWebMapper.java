package com.mineria.equipos.infrastructure.adapter.in.web.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.mineria.equipos.application.port.in.ActualizarEquipoUseCase;
import com.mineria.equipos.application.port.in.CrearEquipoUseCase;
import com.mineria.equipos.application.view.EquipoView;
import com.mineria.equipos.application.view.TipoEquipoView;
import com.mineria.equipos.generated.model.EquipoRequest;
import com.mineria.equipos.generated.model.EquipoResponse;
import com.mineria.equipos.generated.model.TipoEquipoResponse;

/** Convierte entre los DTOs generados por OpenAPI y los comandos/views de la capa de aplicación. */
@Mapper
public interface EquipoWebMapper {

    CrearEquipoUseCase.Comando toCrearComando(EquipoRequest request);

    @Mapping(target = "id", source = "id")
    ActualizarEquipoUseCase.Comando toActualizarComando(Long id, EquipoRequest request);

    // El view viene "aplanado" (tipoId, tipoNombre...); el contrato lo expone como objeto "tipo" anidado
    @Mapping(target = "tipo.id", source = "tipoId")
    @Mapping(target = "tipo.nombre", source = "tipoNombre")
    @Mapping(target = "tipo.intervaloHoras", source = "intervaloHoras")
    EquipoResponse toResponse(EquipoView view);

    List<EquipoResponse> toResponses(List<EquipoView> views);

    TipoEquipoResponse toResponse(TipoEquipoView view);

    List<TipoEquipoResponse> toTipoResponses(List<TipoEquipoView> views);
}
