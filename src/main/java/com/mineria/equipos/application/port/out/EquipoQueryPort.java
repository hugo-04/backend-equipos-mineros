package com.mineria.equipos.application.port.out;

import java.util.List;
import java.util.Optional;

import com.mineria.equipos.application.view.EquipoView;

/** Puerto de salida para las consultas de equipos (lado "lectura"): devuelve views, no modelos. */
public interface EquipoQueryPort {

    List<EquipoView> listar();

    Optional<EquipoView> buscarPorId(Long id);
}
