package com.mineria.equipos.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mineria.equipos.auth.entity.Usuario;

/** Repositorio Spring Data de usuarios. */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByUsername(String username);
}
