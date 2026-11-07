package com.mineria.equipos.application.port.in;

import java.util.List;

import com.mineria.equipos.application.view.TipoEquipoView;

/** Caso de uso: listar el catálogo de tipos de equipo. */
public interface ConsultarTiposUseCase {

    List<TipoEquipoView> listar();
}
