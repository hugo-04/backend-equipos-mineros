package com.mineria.equipos.shared.exception;

/** Pidieron un equipo por id y no existe. */
public class EquipoNotFoundException extends DomainException {

    public static final String CODIGO = "EQUIPO_NO_ENCONTRADO";

    public EquipoNotFoundException(Long id) {
        super(CODIGO, "No existe un equipo con id " + id);
    }
}
