package com.mineria.equipos.infrastructure.security;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Lee el header "Authorization: Bearer ..." y, si el token es válido, deja al usuario autenticado
 * para esa petición. Si el token es inválido o expiró NO responde aquí: guarda el motivo y sigue,
 * y es el entry point quien contesta el 401 con el formato común (así los endpoints públicos
 * siguen funcionando aunque el cliente mande un token viejo).
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** Atributo de la petición donde se deja el código del fallo del token (para el 401). */
    public static final String ATRIBUTO_ERROR_TOKEN = "equipos.error.token";

    private static final String PREFIJO_BEARER = "Bearer ";

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(PREFIJO_BEARER)) {
            try {
                SecurityContextHolder.getContext()
                        .setAuthentication(jwtService.autenticar(header.substring(PREFIJO_BEARER.length()).trim()));
            } catch (TokenJwtException e) {
                log.debug("Token rechazado en {}: {}", request.getRequestURI(), e.getCodigo());
                request.setAttribute(ATRIBUTO_ERROR_TOKEN, e.getCodigo());
            }
        }
        chain.doFilter(request, response);
    }
}
