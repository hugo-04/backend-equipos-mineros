package com.mineria.equipos.application.port.out;

import java.util.Optional;

import com.mineria.equipos.domain.model.TipoEquipo;

/** Puerto de salida para recuperar un tipo de equipo del dominio. */
public interface TipoEquipoRepositoryPort {

    Optional<TipoEquipo> buscarPorId(Long id);
}
