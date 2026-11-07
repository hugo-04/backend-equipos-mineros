package com.mineria.equipos.equipo.policy;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Estado legible de un equipo respecto a su mantenimiento. */
@Getter
@RequiredArgsConstructor
public enum EstadoMantenimiento {

    OK("OK"),
    REQUIERE_MANTENIMIENTO("Requiere mantenimiento");

    private final String etiqueta;

    public static EstadoMantenimiento de(boolean requiereMantenimiento) {
        return requiereMantenimiento ? REQUIERE_MANTENIMIENTO : OK;
    }
}
