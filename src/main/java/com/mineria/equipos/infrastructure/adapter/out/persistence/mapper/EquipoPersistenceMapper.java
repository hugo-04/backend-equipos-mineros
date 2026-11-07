package com.mineria.equipos.infrastructure.adapter.out.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.mineria.equipos.application.view.EquipoView;
import com.mineria.equipos.domain.model.Equipo;
import com.mineria.equipos.infrastructure.adapter.out.persistence.entity.EquipoEntity;
import com.mineria.equipos.infrastructure.adapter.out.persistence.repository.EquipoProjection;

/** Convierte entre la entidad JPA, el modelo de dominio y el modelo de lectura de equipos. */
@Mapper(uses = TipoEquipoPersistenceMapper.class)
public interface EquipoPersistenceMapper {

    Equipo toDomain(EquipoEntity entity);

    EquipoEntity toEntity(Equipo equipo);

    /**
     * El estado de mantenimiento no sale de la base de datos: lo calcula el caso de uso
     * con la política del dominio, por eso se ignoran aquí de forma explícita.
     */
    @Mapping(target = "requiereMantenimiento", ignore = true)
    @Mapping(target = "estado", ignore = true)
    EquipoView toView(EquipoProjection projection);
}
