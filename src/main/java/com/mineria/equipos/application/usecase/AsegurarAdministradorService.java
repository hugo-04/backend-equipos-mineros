package com.mineria.equipos.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mineria.equipos.application.port.in.AsegurarAdministradorUseCase;
import com.mineria.equipos.application.port.out.PasswordHasherPort;
import com.mineria.equipos.application.port.out.UsuarioRepositoryPort;
import com.mineria.equipos.domain.model.Rol;
import com.mineria.equipos.domain.model.Usuario;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Crea el usuario administrador inicial si todavía no existe. Se ejecuta al arrancar con los
 * datos que vienen del entorno, de modo que ninguna credencial viva en el repositorio.
 * Es idempotente: si ya existe no lo toca (no pisa una contraseña cambiada después).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsegurarAdministradorService implements AsegurarAdministradorUseCase {

    private final UsuarioRepositoryPort usuarioRepository;
    private final PasswordHasherPort passwordHasher;

    @Override
    @Transactional
    public boolean asegurar(String username, String password) {
        if (usuarioRepository.buscarPorUsername(username).isPresent()) {
            log.info("El administrador '{}' ya existe, no se crea de nuevo", username);
            return false;
        }
        usuarioRepository.guardar(Usuario.builder()
                .username(username)
                .passwordHash(passwordHasher.hashear(password))
                .rol(Rol.ADMIN)
                .build());
        log.info("Administrador inicial '{}' creado", username);
        return true;
    }
}
