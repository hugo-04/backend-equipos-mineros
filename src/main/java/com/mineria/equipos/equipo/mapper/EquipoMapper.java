package com.mineria.equipos.equipo.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;

import com.mineria.equipos.equipo.entity.Equipo;
import com.mineria.equipos.equipo.policy.EstadoMantenimiento;
import com.mineria.equipos.equipo.policy.MantenimientoPolicy;
import com.mineria.equipos.equipo.repository.projection.EquipoProjection;
import com.mineria.equipos.generated.model.EquipoRequest;
import com.mineria.equipos.generated.model.EquipoResponse;

/**
 * Convierte entre los DTOs del contrato, la entidad y la proyección de equipos. Es una clase abstracta
 * (y no una interfaz) para poder consultar la {@link MantenimientoPolicy} al armar la respuesta:
 * "requiereMantenimiento" y "estado" no se guardan, se calculan al consultar.
 */
@Mapper
public abstract class EquipoMapper {

    @Autowired
    protected MantenimientoPolicy mantenimientoPolicy;

    // La proyección viene "aplanada" (tipoId, tipoNombre...); el contrato la expone con el objeto "tipo" anidado
    @Mapping(target = "tipo.id", source = "tipoId")
    @Mapping(target = "tipo.nombre", source = "tipoNombre")
    @Mapping(target = "tipo.intervaloHoras", source = "intervaloHoras")
    @Mapping(target = "requiereMantenimiento", source = ".", qualifiedByName = "requiereMantenimiento")
    @Mapping(target = "estado", source = ".", qualifiedByName = "estadoLegible")
    public abstract EquipoResponse toResponse(EquipoProjection proyeccion);

    public abstract List<EquipoResponse> toResponses(List<EquipoProjection> proyecciones);

    // El alta fija id, ciclo de mantenimiento y fecha de creación: no vienen del request
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "horometroUltimoMant", ignore = true)
    @Mapping(target = "creadoEn", ignore = true)
    public abstract Equipo toEntity(EquipoRequest request);

    @Named("requiereMantenimiento")
    protected boolean requiereMantenimiento(EquipoProjection p) {
        return mantenimientoPolicy.requiereMantenimiento(p.getHorometro(), p.getHorometroUltimoMant(), p.getIntervaloHoras());
    }

    @Named("estadoLegible")
    protected String estadoLegible(EquipoProjection p) {
        return EstadoMantenimiento.de(requiereMantenimiento(p)).getEtiqueta();
    }
}
