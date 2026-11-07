package com.mineria.equipos.mantenimiento.service;

import org.springframework.data.domain.Pageable;

import com.mineria.equipos.generated.model.MantenimientoRequest;
import com.mineria.equipos.generated.model.MantenimientoResponse;
import com.mineria.equipos.generated.model.PaginaMantenimientoResponse;

/** Servicio del historial de mantenimientos de un equipo. */
public interface MantenimientoService {

    /** Registra un mantenimiento y reinicia el ciclo del equipo, todo en una sola transacción. */
    MantenimientoResponse registrar(Long equipoId, MantenimientoRequest request);

    /** Una página del historial de un equipo, del más reciente al más antiguo. */
    PaginaMantenimientoResponse listarPorEquipo(Long equipoId, Pageable pageable);
}
