package com.mineria.equipos.mantenimiento.service;

import org.springframework.data.domain.Pageable;

import com.mineria.equipos.generated.model.MantenimientoRequest;
import com.mineria.equipos.generated.model.MantenimientoResponse;
import com.mineria.equipos.generated.model.PaginaMantenimientoResponse;

/** Registrar mantenimientos y consultar el historial de un equipo. */
public interface MantenimientoService {

    /** Guarda el mantenimiento y reinicia el ciclo del equipo, las dos cosas en una sola transacción. */
    MantenimientoResponse registrar(Long equipoId, MantenimientoRequest request);

    /** Una página del historial del equipo, empezando por el más reciente. */
    PaginaMantenimientoResponse listarPorEquipo(Long equipoId, Pageable pageable);
}
