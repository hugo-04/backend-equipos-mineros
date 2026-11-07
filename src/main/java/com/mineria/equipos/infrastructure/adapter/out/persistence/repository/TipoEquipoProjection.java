package com.mineria.equipos.infrastructure.adapter.out.persistence.repository;

/** Proyección de lectura de un tipo de equipo. */
public interface TipoEquipoProjection {

    Long getId();

    String getNombre();

    Integer getIntervaloHoras();
}
