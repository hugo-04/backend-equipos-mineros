package com.mineria.equipos.application.port.out;

import java.util.List;

import com.mineria.equipos.application.view.TipoEquipoView;

/** Puerto de salida para consultar el catálogo de tipos de equipo. */
public interface TipoEquipoQueryPort {

    List<TipoEquipoView> listar();
}
