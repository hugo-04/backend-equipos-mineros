package com.mineria.equipos.shared.exception;

/** Se lanza cuando se pide un equipo que no existe. */
public class EquipoNotFoundException extends DomainException {

    public static final String CODIGO = "EQUIPO_NO_ENCONTRADO";

    public EquipoNotFoundException(Long id) {
        super(CODIGO, "No existe un equipo con id " + id);
    }
}
