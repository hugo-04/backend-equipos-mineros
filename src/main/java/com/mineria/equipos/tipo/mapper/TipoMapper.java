package com.mineria.equipos.tipo.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.springframework.data.domain.Page;

import com.mineria.equipos.generated.model.PaginaTipoEquipoResponse;
import com.mineria.equipos.generated.model.TipoEquipoResponse;
import com.mineria.equipos.shared.pagination.MapeoDePagina;
import com.mineria.equipos.tipo.repository.projection.TipoProjection;

/** Pasa la proyección del tipo al DTO que devuelve la API. */
@Mapper
public interface TipoMapper {

    TipoEquipoResponse toResponse(TipoProjection projection);

    List<TipoEquipoResponse> toResponses(List<TipoProjection> projections);

    @MapeoDePagina
    PaginaTipoEquipoResponse toPagina(Page<TipoProjection> pagina);
}
