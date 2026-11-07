package com.mineria.equipos.application.port.in;

import java.math.BigDecimal;

import com.mineria.equipos.application.view.EquipoView;

/** Caso de uso: editar un equipo existente (el horómetro no puede bajar). */
public interface ActualizarEquipoUseCase {

    EquipoView actualizar(Comando comando);

    record Comando(Long id, String codigo, Long tipoId, BigDecimal horometro) {
    }
}
