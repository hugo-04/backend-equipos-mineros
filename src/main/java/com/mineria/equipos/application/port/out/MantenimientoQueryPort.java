package com.mineria.equipos.application.port.out;

import java.util.List;
import java.util.Optional;

import com.mineria.equipos.application.view.MantenimientoView;

/** Puerto de salida para consultar el historial de mantenimientos. */
public interface MantenimientoQueryPort {

    Optional<MantenimientoView> buscarPorId(Long id);

    /** Historial de un equipo, del más reciente al más antiguo. */
    List<MantenimientoView> listarPorEquipo(Long equipoId);
}
