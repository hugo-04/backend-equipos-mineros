package com.mineria.equipos.equipo.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mineria.equipos.equipo.entity.Equipo;
import com.mineria.equipos.equipo.repository.projection.EquipoProjection;

/**
 * Repositorio Spring Data de equipos. Las lecturas del API usan una proyección que une el tipo con
 * un JOIN por {@code tipo_id} (una sola consulta, sin N+1) sin que este módulo dependa de las clases del módulo tipo.
 */
public interface EquipoRepository extends JpaRepository<Equipo, Long> {

    String SELECT_PROYECCION = """
            select e.id as id, e.codigo as codigo,
                   t.id as tipoId, t.nombre as tipoNombre, t.intervaloHoras as intervaloHoras,
                   e.horometro as horometro, e.horometroUltimoMant as horometroUltimoMant,
                   e.creadoEn as creadoEn
            from Equipo e join TipoEquipo t on t.id = e.tipoId
            """;

    /** Una página de equipos con su tipo. El COUNT se escribe a mano porque Spring no lo deriva bien de un JOIN con proyección. */
    @Query(value = SELECT_PROYECCION + " order by e.id",
            countQuery = "select count(e) from Equipo e join TipoEquipo t on t.id = e.tipoId")
    Page<EquipoProjection> listarProyecciones(Pageable pageable);

    @Query(SELECT_PROYECCION + " where e.id = :id")
    Optional<EquipoProjection> buscarProyeccionPorId(@Param("id") Long id);

    boolean existsByCodigo(String codigo);

    /** Sirve al editar: el código puede repetirse solo si pertenece al propio equipo. */
    boolean existsByCodigoAndIdNot(String codigo, Long id);
}
