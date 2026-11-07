package com.mineria.equipos.application.port.in;

import java.math.BigDecimal;

import com.mineria.equipos.application.view.EquipoView;

/** Caso de uso: dar de alta un equipo nuevo. */
public interface CrearEquipoUseCase {

    EquipoView crear(Comando comando);

    record Comando(String codigo, Long tipoId, BigDecimal horometro) {
    }
}
