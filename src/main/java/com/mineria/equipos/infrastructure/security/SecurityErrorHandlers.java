package com.mineria.equipos.infrastructure.security;

import com.mineria.equipos.shared.exception.TokenJwtException;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mineria.equipos.shared.exception.ErrorResponseFactory;
import com.mineria.equipos.generated.model.ErrorResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Respuestas 401 y 403 de la cadena de seguridad. Los errores de seguridad ocurren antes de llegar a
 * los controllers, así que el GlobalExceptionHandler no los ve; estos handlers escriben el mismo
 * {@link ErrorResponse} del contrato para que el cliente siempre reciba un solo formato.
 */
@Component
@RequiredArgsConstructor
public class SecurityErrorHandlers implements AuthenticationEntryPoint, AccessDeniedHandler {

    static final String NO_AUTENTICADO = "NO_AUTENTICADO";
    static final String ACCESO_DENEGADO = "ACCESO_DENEGADO";

    private final ErrorResponseFactory errores;
    private final ObjectMapper objectMapper;

    /** 401: falta el token, es inválido o expiró. */
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
        escribir(response, errores.crear(HttpStatus.UNAUTHORIZED, codigo, mensaje, request.getRequestURI()));
    }

    /** 403: el usuario está autenticado pero su rol no alcanza. */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        escribir(response, errores.crear(HttpStatus.FORBIDDEN, ACCESO_DENEGADO,
                "No tienes permiso para realizar esta operación", request.getRequestURI()));
    }

    private void escribir(HttpServletResponse response, ErrorResponse cuerpo) throws IOException {
        response.setStatus(cuerpo.getStatus());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), cuerpo);
    }
}
