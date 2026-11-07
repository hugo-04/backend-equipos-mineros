package com.mineria.equipos.tipo.repository.projection;

/** Las columnas del tipo tal como salen en el catálogo; es de solo lectura. */
public interface TipoProjection {

    Long getId();

    String getNombre();

    Integer getIntervaloHoras();
}
