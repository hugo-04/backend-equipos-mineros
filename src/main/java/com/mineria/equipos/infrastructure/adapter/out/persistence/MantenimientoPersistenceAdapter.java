package com.mineria.equipos.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.mineria.equipos.application.port.out.MantenimientoQueryPort;
import com.mineria.equipos.application.port.out.MantenimientoRepositoryPort;
import com.mineria.equipos.application.view.MantenimientoView;
import com.mineria.equipos.domain.model.Mantenimiento;
import com.mineria.equipos.infrastructure.adapter.out.persistence.mapper.MantenimientoPersistenceMapper;
import com.mineria.equipos.infrastructure.adapter.out.persistence.repository.MantenimientoJpaRepository;

import lombok.RequiredArgsConstructor;

/** Adaptador que implementa los puertos del historial de mantenimientos con Spring Data JPA. */
@Component
@RequiredArgsConstructor
public class MantenimientoPersistenceAdapter implements MantenimientoRepositoryPort, MantenimientoQueryPort {

    private final MantenimientoJpaRepository repository;
    private final MantenimientoPersistenceMapper mapper;

    @Override
    public Mantenimiento guardar(Mantenimiento mantenimiento) {
        return mapper.toDomain(repository.saveAndFlush(mapper.toEntity(mantenimiento)));
    }

    @Override
    public Optional<MantenimientoView> buscarPorId(Long id) {
        return repository.findProjectedById(id).map(mapper::toView);
    }

    @Override
    public List<MantenimientoView> listarPorEquipo(Long equipoId) {
        return repository.findByEquipoIdOrderByFechaDescIdDesc(equipoId).stream().map(mapper::toView).toList();
    }
}
