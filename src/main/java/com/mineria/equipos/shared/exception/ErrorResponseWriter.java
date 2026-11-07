package com.mineria.equipos.shared.exception;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mineria.equipos.generated.model.ErrorResponse;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Escribe un {@link ErrorResponse} directamente en la respuesta HTTP. Lo usan los filtros y handlers de
 * seguridad (401, 403, 429, 413), que actúan antes de llegar a los controllers y por eso el
 * GlobalExceptionHandler no los ve: así el cliente recibe siempre el mismo formato de error.
 */
@Component
@RequiredArgsConstructor
public class ErrorResponseWriter {

    private final ErrorResponseFactory errores;
    private final ObjectMapper objectMapper;

    public void escribir(HttpServletResponse response, HttpStatus status, String codigo, String mensaje,
                         String path) throws IOException {
        ErrorResponse cuerpo = errores.crear(status, codigo, mensaje, path);
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), cuerpo);
    }
}
