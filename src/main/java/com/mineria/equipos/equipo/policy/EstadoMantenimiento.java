package com.mineria.equipos.equipo.policy;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** El texto con que se muestra si a un equipo le toca mantenimiento o no. */
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
