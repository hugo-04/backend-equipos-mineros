package com.mineria.equipos.tipo.service;

import org.springframework.data.domain.Pageable;

import com.mineria.equipos.generated.model.PaginaTipoEquipoResponse;

/**
 * La puerta de entrada del módulo tipo. Los demás módulos (equipo, por ejemplo) no tocan el repositorio de
 * tipos: si necesitan saber algo de un tipo, se lo preguntan a esta interfaz.
 */
public interface TipoService {

    PaginaTipoEquipoResponse listar(Pageable pageable);

    /** @throws com.mineria.equipos.shared.exception.TipoEquipoNotFoundException si el tipo no existe */
    void verificarExiste(Long id);
}
