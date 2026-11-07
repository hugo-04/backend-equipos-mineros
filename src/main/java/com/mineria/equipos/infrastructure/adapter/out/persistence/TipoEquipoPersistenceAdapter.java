package com.mineria.equipos.infrastructure.adapter.out.persistence;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.mineria.equipos.application.port.out.TipoEquipoRepositoryPort;
import com.mineria.equipos.domain.model.TipoEquipo;
import com.mineria.equipos.infrastructure.adapter.out.persistence.mapper.TipoEquipoPersistenceMapper;
import com.mineria.equipos.tipo.repository.TipoRepository;

import lombok.RequiredArgsConstructor;

/** Puente temporal: sigue sirviendo el puerto de tipos a equipo hasta que migre al módulo equipo. */
@Component
@RequiredArgsConstructor
public class TipoEquipoPersistenceAdapter implements TipoEquipoRepositoryPort {

    private final TipoRepository repository;
    private final TipoEquipoPersistenceMapper mapper;

    @Override
    public Optional<TipoEquipo> buscarPorId(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }
}
