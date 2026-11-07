package com.mineria.equipos.tipo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mineria.equipos.tipo.entity.TipoEquipo;
import com.mineria.equipos.tipo.repository.projection.TipoProjection;

/** Repositorio Spring Data de tipos de equipo. */
public interface TipoRepository extends JpaRepository<TipoEquipo, Long> {

    /** Consulta derivada con proyección: Spring genera el SELECT solo con las 3 columnas. */
    List<TipoProjection> findAllProjectedByOrderByIdAsc();
}
