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
 * Acceso a la tabla equipo. Las lecturas de la API no devuelven la entidad: usan una proyección que trae
 * el tipo con un JOIN por {@code tipo_id}. Es una sola consulta (sin N+1) y este módulo no necesita las
 * clases del módulo tipo para hacerlo.
 */
public interface EquipoRepository extends JpaRepository<Equipo, Long> {

    String SELECT_PROYECCION = """
            select e.id as id, e.codigo as codigo,
                   t.id as tipoId, t.nombre as tipoNombre, t.intervaloHoras as intervaloHoras,
                   e.horometro as horometro, e.horometroUltimoMant as horometroUltimoMant,
                   e.creadoEn as creadoEn
            from Equipo e join TipoEquipo t on t.id = e.tipoId
            """;

    /** Una página de equipos con su tipo. El COUNT va escrito a mano porque Spring no lo deriva bien cuando hay JOIN y proyección. */
    @Query(value = SELECT_PROYECCION + " order by e.id",
            countQuery = "select count(e) from Equipo e join TipoEquipo t on t.id = e.tipoId")
    Page<EquipoProjection> listarProyecciones(Pageable pageable);

    @Query(SELECT_PROYECCION + " where e.id = :id")
    Optional<EquipoProjection> buscarProyeccionPorId(@Param("id") Long id);

    boolean existsByCodigo(String codigo);

    /** Para editar: el código puede "repetirse" únicamente si es el del propio equipo. */
    boolean existsByCodigoAndIdNot(String codigo, Long id);
}
