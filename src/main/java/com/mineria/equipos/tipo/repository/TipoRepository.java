package com.mineria.equipos.tipo.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.mineria.equipos.tipo.entity.TipoEquipo;
import com.mineria.equipos.tipo.repository.projection.TipoProjection;

/** Repositorio Spring Data de tipos de equipo. */
public interface TipoRepository extends JpaRepository<TipoEquipo, Long> {

    /** Consulta derivada con proyección y paginación: Spring genera el SELECT (y el COUNT) solo con lo necesario. */
    Page<TipoProjection> findAllProjectedByOrderByIdAsc(Pageable pageable);
}
