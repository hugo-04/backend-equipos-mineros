package com.mineria.equipos.equipo.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;

import com.mineria.equipos.equipo.entity.Equipo;
import com.mineria.equipos.equipo.policy.EstadoMantenimiento;
import com.mineria.equipos.equipo.policy.MantenimientoPolicy;
import com.mineria.equipos.equipo.repository.projection.EquipoProjection;
import com.mineria.equipos.generated.model.EquipoRequest;
import com.mineria.equipos.generated.model.EquipoResponse;
import com.mineria.equipos.generated.model.PaginaEquipoResponse;
import com.mineria.equipos.shared.pagination.MapeoDePagina;

/**
 * Pasa de los DTOs del contrato a la entidad y de la proyección al DTO. Es clase abstracta y no interfaz
 * porque necesita la {@link MantenimientoPolicy} para armar la respuesta: "requiereMantenimiento" y "estado"
 * no están en la base, se calculan cada vez que se consulta.
 */
@Mapper
public abstract class EquipoMapper {

    @Autowired
    protected MantenimientoPolicy mantenimientoPolicy;

    // La proyección llega aplanada (tipoId, tipoNombre...) y el contrato pide el objeto "tipo" anidado
    @Mapping(target = "tipo.id", source = "tipoId")
    @Mapping(target = "tipo.nombre", source = "tipoNombre")
    @Mapping(target = "tipo.intervaloHoras", source = "intervaloHoras")
    @Mapping(target = "requiereMantenimiento", source = ".", qualifiedByName = "requiereMantenimiento")
    @Mapping(target = "estado", source = ".", qualifiedByName = "estadoLegible")
    public abstract EquipoResponse toResponse(EquipoProjection proyeccion);

    public abstract List<EquipoResponse> toResponses(List<EquipoProjection> proyecciones);

    @MapeoDePagina
    public abstract PaginaEquipoResponse toPagina(Page<EquipoProjection> pagina);

    // Esos tres no vienen del request: el id lo pone la base, el ciclo arranca en 0 y la fecha la pone el service
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "horometroUltimoMant", ignore = true)
    @Mapping(target = "creadoEn", ignore = true)
    public abstract Equipo toEntity(EquipoRequest request);

    // Al editar solo se copian código, tipo y horómetro; el id, el ciclo y la fecha de creación se quedan como estaban
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "horometroUltimoMant", ignore = true)
    @Mapping(target = "creadoEn", ignore = true)
    public abstract void actualizar(EquipoRequest request, @MappingTarget Equipo equipo);

    @Named("requiereMantenimiento")
    protected boolean requiereMantenimiento(EquipoProjection p) {
        return mantenimientoPolicy.requiereMantenimiento(p.getHorometro(), p.getHorometroUltimoMant(), p.getIntervaloHoras());
    }

    @Named("estadoLegible")
    protected String estadoLegible(EquipoProjection p) {
        return EstadoMantenimiento.de(requiereMantenimiento(p)).getEtiqueta();
    }
}
