package com.mineria.equipos.shared.exception;

/** El tipo de equipo que mandaron no está en el catálogo. */
public class TipoEquipoNotFoundException extends DomainException {

    public static final String CODIGO = "TIPO_EQUIPO_NO_ENCONTRADO";

    public TipoEquipoNotFoundException(Long id) {
        super(CODIGO, "No existe un tipo de equipo con id " + id);
    }
}
