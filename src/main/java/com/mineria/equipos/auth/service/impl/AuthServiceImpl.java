package com.mineria.equipos.auth.service.impl;

import java.util.UUID;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mineria.equipos.auth.entity.Rol;
import com.mineria.equipos.auth.mapper.AuthMapper;
import com.mineria.equipos.auth.repository.UsuarioRepository;
import com.mineria.equipos.auth.security.JwtService;
import com.mineria.equipos.auth.service.AuthService;
import com.mineria.equipos.generated.model.LoginRequest;
import com.mineria.equipos.generated.model.LoginResponse;
import com.mineria.equipos.shared.exception.CredencialesInvalidasException;

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
public class AuthServiceImpl implements AuthService {

    private static final String TIPO_TOKEN = "Bearer";

    private final UserDetailsService userDetailsService;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthMapper mapper;

    private volatile String hashSenuelo;

    @Override
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        try {
            UserDetails usuario = cargarUsuario(request.getUsername(), request.getPassword());
            if (!passwordEncoder.matches(request.getPassword(), usuario.getPassword())) {
                throw new CredencialesInvalidasException();
            }

            log.info("Login correcto: usuario={}", usuario.getUsername());
            return mapper.toLoginResponse(jwtService.generarToken(usuario), TIPO_TOKEN, jwtService.getExpiracionSegundos());
        } catch (CredencialesInvalidasException e) {
            log.warn("Login rechazado para el usuario {}", request.getUsername());
            throw e;
        } catch (RuntimeException e) {
            log.error("Fallo inesperado durante el login del usuario {}", request.getUsername(), e);
            throw e;
        }
    }

    /**
     * Crea el administrador inicial si todavía no existe. Se ejecuta al arrancar con los datos que vienen
     * del entorno, de modo que ninguna credencial viva en el repositorio.
     * Es idempotente: si ya existe no lo toca (no pisa una contraseña cambiada después).
     */
    @Override
    @Transactional
    public boolean asegurarAdministrador(String username, String password) {
        if (usuarioRepository.findByUsername(username).isPresent()) {
            log.info("El administrador '{}' ya existe, no se crea de nuevo", username);
            return false;
        }
        usuarioRepository.save(mapper.toEntity(username, passwordEncoder.encode(password), Rol.ADMIN));
        log.info("Administrador inicial '{}' creado", username);
        return true;
    }

    private UserDetails cargarUsuario(String username, String password) {
        try {
            return userDetailsService.loadUserByUsername(username);
        } catch (UsernameNotFoundException e) {
            // Mismo error que una clave incorrecta y, además, el mismo tiempo: se hace el mismo trabajo de BCrypt
            // contra un hash señuelo, así medir cuánto tarda el login no revela qué usuarios existen
            passwordEncoder.matches(password, hashSenuelo());
            throw new CredencialesInvalidasException();
        }
    }

    /** Hash BCrypt de una clave aleatoria, con el mismo costo que los hashes reales. Se calcula una sola vez. */
    private String hashSenuelo() {
        String hash = hashSenuelo;
        if (hash == null) {
            hash = passwordEncoder.encode(UUID.randomUUID().toString());
            hashSenuelo = hash;
        }
        return hash;
    }
}
