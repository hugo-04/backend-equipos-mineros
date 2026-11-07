package com.mineria.equipos.application.port.in;

import java.util.List;

import com.mineria.equipos.application.view.EquipoView;

/** Casos de uso de lectura sobre equipos (relacionados, por eso comparten interfaz). */
public interface ConsultarEquiposUseCase {

    List<EquipoView> listar();

    EquipoView obtener(Long id);
}
