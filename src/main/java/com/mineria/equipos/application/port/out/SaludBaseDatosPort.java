package com.mineria.equipos.application.port.out;

/** Puerto de salida para saber si la base de datos responde. */
public interface SaludBaseDatosPort {

    boolean estaDisponible();
}
