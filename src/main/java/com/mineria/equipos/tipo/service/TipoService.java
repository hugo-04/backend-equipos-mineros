package com.mineria.equipos.tipo.service;

import java.util.List;

import com.mineria.equipos.generated.model.TipoEquipoResponse;

/**
 * Servicio del catálogo de tipos de equipo. Es la única puerta de entrada al módulo para el resto de la
 * aplicación: otros módulos (por ejemplo equipo) preguntan por un tipo a través de esta interfaz.
 */
public interface TipoService {

    List<TipoEquipoResponse> listar();

    /** @throws com.mineria.equipos.shared.exception.TipoEquipoNotFoundException si el tipo no existe */
    void verificarExiste(Long id);
}
