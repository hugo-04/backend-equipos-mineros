package com.mineria.equipos.infrastructure.adapter.out.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mineria.equipos.infrastructure.adapter.out.persistence.entity.MantenimientoEntity;

/** Repositorio Spring Data del historial de mantenimientos. */
public interface MantenimientoJpaRepository extends JpaRepository<MantenimientoEntity, Long> {

    /** El más reciente primero; el id desempata dos mantenimientos con la misma fecha. */
    List<MantenimientoProjection> findByEquipoIdOrderByFechaDescIdDesc(Long equipoId);

    Optional<MantenimientoProjection> findProjectedById(Long id);
}
