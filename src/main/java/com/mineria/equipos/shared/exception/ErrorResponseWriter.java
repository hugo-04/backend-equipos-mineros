package com.mineria.equipos.shared.exception;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import tools.jackson.databind.ObjectMapper;
import com.mineria.equipos.generated.model.ErrorResponse;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Escribe el {@link ErrorResponse} directo en la respuesta HTTP. Los filtros de seguridad (401, 403, 429
 * y 413) cortan la petición antes de llegar a un controller, y entonces el GlobalExceptionHandler ni se
 * entera; por eso necesitan esto para devolver el mismo formato de error que el resto.
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
