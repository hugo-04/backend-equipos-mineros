package com.mineria.equipos.shared.exception;

/**
 * El tipo de un equipo no se cambia una vez creado: el estado y los mantenimientos se rigen por el intervalo
 * de ese tipo, y cambiarlo después dejaría el historial sin sentido. Si alguien se equivocó, se elimina el
 * equipo y se crea otro. El contrato ya impide mandar el tipo al editar; esta excepción es la segunda defensa.
 */
public class TipoNoModificableException extends DomainException {

    public static final String CODIGO = "TIPO_NO_MODIFICABLE";

    public TipoNoModificableException() {
        super(CODIGO, "El tipo de un equipo no se puede cambiar. Elimina el equipo y créalo de nuevo con el tipo correcto");
    }
}
