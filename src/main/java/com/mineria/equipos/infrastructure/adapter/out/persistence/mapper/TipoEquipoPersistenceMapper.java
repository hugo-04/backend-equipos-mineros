package com.mineria.equipos.infrastructure.adapter.out.persistence.mapper;

import org.mapstruct.Mapper;

import com.mineria.equipos.domain.model.TipoEquipo;

/** Puente temporal entre el tipo de dominio y la entidad del módulo tipo (se elimina al migrar equipo). */
@Mapper
public interface TipoEquipoPersistenceMapper {

    TipoEquipo toDomain(com.mineria.equipos.tipo.entity.TipoEquipo entity);

    com.mineria.equipos.tipo.entity.TipoEquipo toEntity(TipoEquipo tipo);
}
