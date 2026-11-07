package com.mineria.equipos.infrastructure.adapter.out.persistence;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.mineria.equipos.application.port.out.UsuarioRepositoryPort;
import com.mineria.equipos.domain.model.Usuario;
import com.mineria.equipos.infrastructure.adapter.out.persistence.mapper.UsuarioPersistenceMapper;
import com.mineria.equipos.infrastructure.adapter.out.persistence.repository.UsuarioJpaRepository;

import lombok.RequiredArgsConstructor;

/** Adaptador que implementa el puerto de usuarios con Spring Data JPA. */
@Component
@RequiredArgsConstructor
public class UsuarioPersistenceAdapter implements UsuarioRepositoryPort {

    private final UsuarioJpaRepository repository;
    private final UsuarioPersistenceMapper mapper;

    @Override
    public Optional<Usuario> buscarPorUsername(String username) {
        return repository.findByUsername(username).map(mapper::toDomain);
    }

    @Override
    public Usuario guardar(Usuario usuario) {
        return mapper.toDomain(repository.save(mapper.toEntity(usuario)));
    }
}
