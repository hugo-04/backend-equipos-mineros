package com.mineria.equipos.mantenimiento.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mineria.equipos.mantenimiento.entity.Mantenimiento;
import com.mineria.equipos.mantenimiento.repository.projection.MantenimientoProjection;

/** Repositorio Spring Data del historial de mantenimientos. */
public interface MantenimientoRepository extends JpaRepository<Mantenimiento, Long> {

    /** El más reciente primero; el id desempata dos mantenimientos con la misma fecha. */
    List<MantenimientoProjection> findByEquipoIdOrderByFechaDescIdDesc(Long equipoId);

    Optional<MantenimientoProjection> findProjectedById(Long id);
}
