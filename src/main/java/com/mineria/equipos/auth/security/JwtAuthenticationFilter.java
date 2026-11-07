package com.mineria.equipos.auth.security;

import com.mineria.equipos.shared.exception.TokenJwtException;
import com.mineria.equipos.shared.trace.LogSeguro;

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
 * Lee el header "Authorization: Bearer ..." y, si el token sirve, deja al usuario autenticado en esa
 * petición. Si el token está vencido o es inválido no responde acá: anota el motivo y deja seguir. El 401
 * lo contesta el entry point, con el formato de siempre. Así los endpoints públicos siguen funcionando
 * aunque el cliente mande un token viejo.
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** Donde se anota, en la petición, por qué falló el token, para que el 401 lo explique. */
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
                log.debug("Token rechazado en {}: {}", LogSeguro.limpiar(request.getRequestURI()), e.getCodigo());
                request.setAttribute(ATRIBUTO_ERROR_TOKEN, e.getCodigo());
            } catch (RuntimeException e) {
                // Falla cerrado: si pasa algo raro al validar el token, la petición queda sin autenticar y sale 401
                log.error("Error inesperado al validar el token en {}", LogSeguro.limpiar(request.getRequestURI()), e);
                request.setAttribute(ATRIBUTO_ERROR_TOKEN, TokenJwtException.TOKEN_INVALIDO);
            }
        }
        chain.doFilter(request, response);
    }
}
