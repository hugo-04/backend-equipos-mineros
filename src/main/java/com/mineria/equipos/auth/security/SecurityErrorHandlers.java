package com.mineria.equipos.auth.security;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.mineria.equipos.shared.exception.ErrorResponseWriter;
import com.mineria.equipos.shared.exception.TokenJwtException;
import com.mineria.equipos.shared.trace.LogSeguro;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Responde los 401 y 403 de la cadena de seguridad. Esos errores pasan antes de llegar a un controller,
 * así que el GlobalExceptionHandler no los ve; estos handlers escriben el mismo {@code ErrorResponse} para
 * que el cliente reciba siempre un solo formato.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityErrorHandlers implements AuthenticationEntryPoint, AccessDeniedHandler {

    static final String NO_AUTENTICADO = "NO_AUTENTICADO";
    static final String ACCESO_DENEGADO = "ACCESO_DENEGADO";

    private final ErrorResponseWriter escritor;

    /** 401: no mandaron el token, es inválido o ya venció. */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        Object motivoToken = request.getAttribute(JwtAuthenticationFilter.ATRIBUTO_ERROR_TOKEN);
        String codigo = motivoToken != null ? motivoToken.toString() : NO_AUTENTICADO;
        String mensaje = switch (codigo) {
            case TokenJwtException.TOKEN_EXPIRADO -> "El token expiró, inicia sesión de nuevo";
            case TokenJwtException.TOKEN_INVALIDO -> "El token no es válido";
            default -> "Se requiere un token en el header Authorization: Bearer <token>";
        };
        log.warn("Seguridad: acceso no autenticado {} {} motivo={}", request.getMethod(),
                LogSeguro.limpiar(request.getRequestURI()), codigo);
        escritor.escribir(response, HttpStatus.UNAUTHORIZED, codigo, mensaje, request.getRequestURI());
    }

    /** 403: el usuario sí inició sesión, pero su rol no le alcanza para esto. */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        log.warn("Seguridad: acceso denegado {} {} usuario={}", request.getMethod(),
                LogSeguro.limpiar(request.getRequestURI()),
                autenticacion != null ? LogSeguro.limpiar(autenticacion.getName()) : "-");
        escritor.escribir(response, HttpStatus.FORBIDDEN, ACCESO_DENEGADO,
                "No tienes permiso para realizar esta operación", request.getRequestURI());
    }
}
