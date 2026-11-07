package com.mineria.equipos.application.view;

import lombok.Builder;
import lombok.Value;

/** Modelo de lectura de un tipo de equipo, tal como se muestra en el catálogo. */
@Value
@Builder
public class TipoEquipoView {

    Long id;
    String nombre;
    int intervaloHoras;
}
