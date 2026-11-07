package com.mineria.equipos.infrastructure.adapter.out.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mineria.equipos.infrastructure.adapter.out.persistence.entity.EquipoEntity;

/** Repositorio Spring Data de equipos. Las lecturas del API usan proyecciones con JOIN al tipo. */
public interface EquipoJpaRepository extends JpaRepository<EquipoEntity, Long> {

    String SELECT_PROYECCION = """
            select e.id as id, e.codigo as codigo,
                   t.id as tipoId, t.nombre as tipoNombre, t.intervaloHoras as intervaloHoras,
                   e.horometro as horometro, e.horometroUltimoMant as horometroUltimoMant,
                   e.creadoEn as creadoEn
            from EquipoEntity e join e.tipo t
            """;

    @Query(SELECT_PROYECCION + " order by e.id")
    List<EquipoProjection> listarProyecciones();

    @Query(SELECT_PROYECCION + " where e.id = :id")
    Optional<EquipoProjection> buscarProyeccionPorId(@Param("id") Long id);

    /** Para escribir: trae el equipo con su tipo en una sola consulta (JOIN FETCH, sin N+1). */
    @Query("select e from EquipoEntity e join fetch e.tipo where e.id = :id")
    Optional<EquipoEntity> buscarConTipo(@Param("id") Long id);

    boolean existsByCodigo(String codigo);

    boolean existsByCodigoAndIdNot(String codigo, Long id);
}
