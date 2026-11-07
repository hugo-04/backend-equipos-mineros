package com.mineria.equipos.equipo.service;

import java.math.BigDecimal;
import java.util.List;

import com.mineria.equipos.generated.model.EquipoRequest;
import com.mineria.equipos.generated.model.EquipoResponse;

/**
 * Servicio de equipos. Es la única puerta de entrada al módulo: el controller y otros módulos
 * (por ejemplo mantenimiento) usan esta interfaz y nunca el repositorio ni la entidad.
 */
public interface EquipoService {

    List<EquipoResponse> listar();

    EquipoResponse obtener(Long id);

    EquipoResponse crear(EquipoRequest request);

    EquipoResponse actualizar(Long id, EquipoRequest request);

    void eliminar(Long id);

    /** @throws com.mineria.equipos.shared.exception.EquipoNotFoundException si el equipo no existe */
    void verificarExiste(Long id);

    /**
     * Reinicia el ciclo de mantenimiento del equipo: su horómetro actual pasa a ser el del último mantenimiento.
     * Se ejecuta dentro de la transacción de quien lo llama (por ejemplo, registrar un mantenimiento).
     *
     * @return el horómetro del equipo en el momento del reinicio
     * @throws com.mineria.equipos.shared.exception.EquipoNotFoundException si el equipo no existe
     */
    BigDecimal reiniciarCiclo(Long id);
}
