package com.mineria.equipos.mantenimiento.service;

import java.util.List;

import com.mineria.equipos.generated.model.MantenimientoRequest;
import com.mineria.equipos.generated.model.MantenimientoResponse;

/** Servicio del historial de mantenimientos de un equipo. */
public interface MantenimientoService {

    /** Registra un mantenimiento y reinicia el ciclo del equipo, todo en una sola transacción. */
    MantenimientoResponse registrar(Long equipoId, MantenimientoRequest request);

    /** Historial de un equipo, del más reciente al más antiguo. */
    List<MantenimientoResponse> listarPorEquipo(Long equipoId);
}
