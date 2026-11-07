package com.mineria.equipos.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.mineria.equipos.application.port.out.TipoEquipoQueryPort;
import com.mineria.equipos.application.port.out.TipoEquipoRepositoryPort;
import com.mineria.equipos.application.view.TipoEquipoView;
import com.mineria.equipos.domain.model.TipoEquipo;
import com.mineria.equipos.infrastructure.adapter.out.persistence.mapper.TipoEquipoPersistenceMapper;
import com.mineria.equipos.infrastructure.adapter.out.persistence.repository.TipoEquipoJpaRepository;

import lombok.RequiredArgsConstructor;

/** Adaptador que implementa los puertos del catálogo de tipos de equipo con Spring Data JPA. */
@Component
@RequiredArgsConstructor
public class TipoEquipoPersistenceAdapter implements TipoEquipoRepositoryPort, TipoEquipoQueryPort {

    private final TipoEquipoJpaRepository repository;
    private final TipoEquipoPersistenceMapper mapper;

    @Override
    public Optional<TipoEquipo> buscarPorId(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<TipoEquipoView> listar() {
        return repository.findAllProjectedByOrderByIdAsc().stream().map(mapper::toView).toList();
    }
}
