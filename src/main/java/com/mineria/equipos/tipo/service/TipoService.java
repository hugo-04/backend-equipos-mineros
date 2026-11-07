package com.mineria.equipos.tipo.service;

import org.springframework.data.domain.Pageable;

import com.mineria.equipos.generated.model.PaginaTipoEquipoResponse;

/**
 * Servicio del catálogo de tipos de equipo (paginado). Es la única puerta de entrada al módulo para el resto de la
 * aplicación: otros módulos (por ejemplo equipo) preguntan por un tipo a través de esta interfaz.
 */
public interface TipoService {

    PaginaTipoEquipoResponse listar(Pageable pageable);

    /** @throws com.mineria.equipos.shared.exception.TipoEquipoNotFoundException si el tipo no existe */
    void verificarExiste(Long id);
}
