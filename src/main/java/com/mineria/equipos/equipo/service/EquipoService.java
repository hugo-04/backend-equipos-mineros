package com.mineria.equipos.equipo.service;

import java.math.BigDecimal;

import org.springframework.data.domain.Pageable;

import com.mineria.equipos.generated.model.EquipoRequest;
import com.mineria.equipos.generated.model.EquipoResponse;
import com.mineria.equipos.generated.model.EquipoUpdateRequest;
import com.mineria.equipos.generated.model.PaginaEquipoResponse;

/**
 * La puerta de entrada del módulo equipo. El controller y los otros módulos (mantenimiento, por ejemplo)
 * pasan por esta interfaz; ninguno toca el repositorio ni la entidad.
 */
public interface EquipoService {

    PaginaEquipoResponse listar(Pageable pageable);

    EquipoResponse obtener(Long id);

    EquipoResponse crear(EquipoRequest request);

    /** Edita código y horómetro. El tipo no se puede cambiar: ver {@link #actualizar(Long, EquipoUpdateRequest, Long)}. */
    default EquipoResponse actualizar(Long id, EquipoUpdateRequest request) {
        return actualizar(id, request, null);
    }

    /**
     * Igual que la anterior, pero con el tipo que alguien quiso poner (si lo hubo). La API nunca lo manda porque
     * el contrato lo rechaza con 400; este parámetro es la segunda defensa, por si otro código intenta cambiarlo.
     * Mandar el mismo tipo que ya tiene el equipo no es un cambio y se acepta.
     *
     * @param tipoIdSolicitado el tipo que se quiso asignar, o null si no se tocó
     * @throws com.mineria.equipos.shared.exception.TipoNoModificableException si es distinto al tipo actual
     * @throws com.mineria.equipos.shared.exception.HorometroNoPuedeBajarException si el horómetro es menor al actual
     * @throws com.mineria.equipos.shared.exception.CodigoDuplicadoException si el código ya es de otro equipo
     */
    EquipoResponse actualizar(Long id, EquipoUpdateRequest request, Long tipoIdSolicitado);

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
