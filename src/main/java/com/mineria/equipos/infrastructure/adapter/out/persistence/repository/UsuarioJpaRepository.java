package com.mineria.equipos.infrastructure.adapter.out.persistence.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mineria.equipos.infrastructure.adapter.out.persistence.entity.UsuarioEntity;

/** Repositorio Spring Data de usuarios. */
public interface UsuarioJpaRepository extends JpaRepository<UsuarioEntity, Long> {

    Optional<UsuarioEntity> findByUsername(String username);
}
