package com.mineria.equipos.application.port.in;

import com.mineria.equipos.application.view.MantenimientoView;

/** Caso de uso: registrar un mantenimiento y reiniciar el ciclo del equipo. */
public interface RegistrarMantenimientoUseCase {

    MantenimientoView registrar(Comando comando);

    record Comando(Long equipoId, String notas) {
    }
}
