package com.mineria.equipos.equipo.service;

import java.math.BigDecimal;

import org.springframework.data.domain.Pageable;

import com.mineria.equipos.generated.model.EquipoRequest;
import com.mineria.equipos.generated.model.EquipoResponse;
import com.mineria.equipos.generated.model.PaginaEquipoResponse;

/**
 * La puerta de entrada del módulo equipo. El controller y los otros módulos (mantenimiento, por ejemplo)
 * pasan por esta interfaz; ninguno toca el repositorio ni la entidad.
 */
public interface EquipoService {

    PaginaEquipoResponse listar(Pageable pageable);

    EquipoResponse obtener(Long id);

    EquipoResponse crear(EquipoRequest request);

    EquipoResponse actualizar(Long id, EquipoRequest request);

    void eliminar(Long id);

    /** @throws com.mineria.equipos.shared.exception.EquipoNotFoundException si el equipo no existe */
    void verificarExiste(Long id);

    /**
     * Reinicia el ciclo de mantenimiento: el horómetro de ahora queda como el del último mantenimiento y la
     * cuenta de horas vuelve a empezar. Corre dentro de la transacción de quien lo llama (al registrar un
     * mantenimiento), así se deshace junto con el historial si algo falla.
     *
     * @return el horómetro del equipo en el momento del reinicio
     * @throws com.mineria.equipos.shared.exception.EquipoNotFoundException si el equipo no existe
     */
    BigDecimal reiniciarCiclo(Long id);
}
