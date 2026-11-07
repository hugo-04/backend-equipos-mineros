package com.mineria.equipos.application.port.in;

/** Caso de uso: eliminar un equipo junto con todo su historial de mantenimientos. */
public interface EliminarEquipoUseCase {

    void eliminar(Long id);
}
