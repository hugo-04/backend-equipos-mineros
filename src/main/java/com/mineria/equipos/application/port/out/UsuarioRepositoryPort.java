package com.mineria.equipos.application.port.out;

import java.util.Optional;

import com.mineria.equipos.domain.model.Usuario;

/** Puerto de salida para guardar y buscar usuarios. */
public interface UsuarioRepositoryPort {

    Optional<Usuario> buscarPorUsername(String username);

    Usuario guardar(Usuario usuario);
}
