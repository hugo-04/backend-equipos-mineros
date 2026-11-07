package com.mineria.equipos.tipo.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.mineria.equipos.tipo.entity.TipoEquipo;
import com.mineria.equipos.tipo.repository.projection.TipoProjection;

/** Acceso a la tabla tipo_equipo. */
public interface TipoRepository extends JpaRepository<TipoEquipo, Long> {

    /** Devuelve la proyección y no la entidad, así el SELECT trae solo las columnas que se muestran. El COUNT lo arma Spring. */
    Page<TipoProjection> findAllProjectedByOrderByIdAsc(Pageable pageable);
}
