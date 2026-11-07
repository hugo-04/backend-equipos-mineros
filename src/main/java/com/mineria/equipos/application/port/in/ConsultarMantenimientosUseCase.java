package com.mineria.equipos.application.port.in;

import java.util.List;

import com.mineria.equipos.application.view.MantenimientoView;

/** Caso de uso: ver el historial de mantenimientos de un equipo. */
public interface ConsultarMantenimientosUseCase {

    List<MantenimientoView> listarPorEquipo(Long equipoId);
}
