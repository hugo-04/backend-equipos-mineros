package com.mineria.equipos.infrastructure.adapter.out.persistence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mineria.equipos.infrastructure.adapter.out.persistence.entity.TipoEquipoEntity;

/** Repositorio Spring Data de tipos de equipo. */
public interface TipoEquipoJpaRepository extends JpaRepository<TipoEquipoEntity, Long> {

    /** Consulta derivada con proyección: Spring genera el SELECT solo con las 3 columnas. */
    List<TipoEquipoProjection> findAllProjectedByOrderByIdAsc();
}
