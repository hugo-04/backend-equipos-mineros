package com.mineria.equipos.infrastructure.adapter.out.persistence.mapper;

import org.mapstruct.Mapper;

import com.mineria.equipos.application.view.TipoEquipoView;
import com.mineria.equipos.domain.model.TipoEquipo;
import com.mineria.equipos.infrastructure.adapter.out.persistence.entity.TipoEquipoEntity;
import com.mineria.equipos.infrastructure.adapter.out.persistence.repository.TipoEquipoProjection;

/** Convierte entre la entidad JPA, el modelo de dominio y el modelo de lectura de tipos. */
@Mapper
public interface TipoEquipoPersistenceMapper {

    TipoEquipo toDomain(TipoEquipoEntity entity);

    TipoEquipoEntity toEntity(TipoEquipo tipo);

    TipoEquipoView toView(TipoEquipoProjection projection);
}
