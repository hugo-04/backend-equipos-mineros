package com.mineria.equipos.tipo.repository.projection;

/** Proyección de solo lectura de un tipo de equipo, tal como se muestra en el catálogo. */
public interface TipoProjection {

    Long getId();

    String getNombre();

    Integer getIntervaloHoras();
}
