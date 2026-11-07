package com.mineria.equipos.application.port.out;

import java.util.Optional;

import com.mineria.equipos.domain.model.Equipo;

/** Puerto de salida para guardar y recuperar equipos del dominio (lado "escritura"). */
public interface EquipoRepositoryPort {

    Optional<Equipo> buscarPorId(Long id);

    Equipo guardar(Equipo equipo);

    boolean existePorId(Long id);

    boolean existePorCodigo(String codigo);

    /** Sirve al editar: el código puede repetirse solo si pertenece al propio equipo. */
    boolean existePorCodigoDistintoDe(String codigo, Long id);

    void eliminarPorId(Long id);
}
