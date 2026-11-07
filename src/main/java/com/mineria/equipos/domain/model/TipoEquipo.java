package com.mineria.equipos.domain.model;

import lombok.Builder;
import lombok.Value;

/**
 * Categoría de un equipo (camión, excavadora, perforadora...).
 * Define cada cuántas horas de uso el equipo necesita mantenimiento.
 */
@Value
@Builder(toBuilder = true)
public class TipoEquipo {

    Long id;
    String nombre;
    int intervaloHoras;
}
