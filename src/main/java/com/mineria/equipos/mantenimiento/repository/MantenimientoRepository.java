package com.mineria.equipos.mantenimiento.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.mineria.equipos.mantenimiento.entity.Mantenimiento;
import com.mineria.equipos.mantenimiento.repository.projection.MantenimientoProjection;

/** Acceso a la tabla mantenimiento. */
public interface MantenimientoRepository extends JpaRepository<Mantenimiento, Long> {

    /** Del más reciente al más antiguo. Si dos tienen la misma fecha, desempata el id. */
    Page<MantenimientoProjection> findByEquipoIdOrderByFechaDescIdDesc(Long equipoId, Pageable pageable);

    Optional<MantenimientoProjection> findProjectedById(Long id);
}
