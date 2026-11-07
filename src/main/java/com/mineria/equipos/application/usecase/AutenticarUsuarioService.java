package com.mineria.equipos.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mineria.equipos.application.port.in.AutenticarUsuarioUseCase;
import com.mineria.equipos.application.port.out.PasswordHasherPort;
import com.mineria.equipos.application.port.out.TokenProviderPort;
import com.mineria.equipos.application.port.out.UsuarioRepositoryPort;
import com.mineria.equipos.application.view.SesionView;
import com.mineria.equipos.domain.exception.CredencialesInvalidasException;
import com.mineria.equipos.domain.model.Usuario;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Valida usuario y contraseña contra la base de datos y emite el token de sesión.
 * Si el usuario no existe o la clave no coincide responde exactamente el mismo error,
 * para no dar pistas sobre qué usuarios existen.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AutenticarUsuarioService implements AutenticarUsuarioUseCase {

    private final UsuarioRepositoryPort usuarioRepository;
    private final PasswordHasherPort passwordHasher;
    private final TokenProviderPort tokenProvider;

    @Override
    @Transactional(readOnly = true)
    public SesionView autenticar(Comando comando) {
        try {
            Usuario usuario = usuarioRepository.buscarPorUsername(comando.username())
                    .filter(u -> passwordHasher.coincide(comando.password(), u.getPasswordHash()))
                    .orElseThrow(CredencialesInvalidasException::new);

            log.info("Login correcto: usuario={}", usuario.getUsername());
            return tokenProvider.emitir(usuario);
        } catch (CredencialesInvalidasException e) {
            log.warn("Login rechazado para el usuario {}", comando.username());
            throw e;
        } catch (RuntimeException e) {
            log.error("Fallo inesperado durante el login del usuario {}", comando.username(), e);
            throw e;
        }
    }
}
