package com.mineria.equipos.infrastructure.adapter.out.persistence.mapper;

import org.mapstruct.Mapper;

import com.mineria.equipos.application.view.MantenimientoView;
import com.mineria.equipos.domain.model.Mantenimiento;
import com.mineria.equipos.infrastructure.adapter.out.persistence.entity.MantenimientoEntity;
import com.mineria.equipos.infrastructure.adapter.out.persistence.repository.MantenimientoProjection;

/** Convierte entre la entidad JPA, el modelo de dominio y el modelo de lectura del historial. */
@Mapper
public interface MantenimientoPersistenceMapper {

    Mantenimiento toDomain(MantenimientoEntity entity);

    MantenimientoEntity toEntity(Mantenimiento mantenimiento);

    MantenimientoView toView(MantenimientoProjection projection);
}
