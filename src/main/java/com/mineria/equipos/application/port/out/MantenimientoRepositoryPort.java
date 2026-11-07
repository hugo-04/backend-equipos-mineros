package com.mineria.equipos.application.port.out;

import com.mineria.equipos.domain.model.Mantenimiento;

/** Puerto de salida para guardar una línea del historial de mantenimientos. */
public interface MantenimientoRepositoryPort {

    Mantenimiento guardar(Mantenimiento mantenimiento);
}
