package com.mineria.equipos.shared.config;

import java.io.IOException;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import com.mineria.equipos.shared.exception.ErrorResponseWriter;
import com.mineria.equipos.shared.trace.LogSeguro;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Corta con 413 las peticiones cuyo cuerpo pasa del máximo (16 KB por defecto, de sobra para los JSON de esta
 * API) antes de que Jackson se ponga a leerlo. Si el cuerpo viene en chunked, sin Content-Length, también lo
 * rechazo: no hay forma de saber su tamaño de antemano. Solo se fija en POST, PUT y PATCH.
 */
@Slf4j
@RequiredArgsConstructor
public class LimiteCuerpoFilter extends OncePerRequestFilter {

    static final String CODIGO = "CUERPO_DEMASIADO_GRANDE";

    private final long maxBytes;
    private final ErrorResponseWriter escritor;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String metodo = request.getMethod();
        return !(HttpMethod.POST.matches(metodo) || HttpMethod.PUT.matches(metodo) || HttpMethod.PATCH.matches(metodo));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        long declarado = request.getContentLengthLong();
        boolean sinLongitud = declarado < 0 && request.getHeader("Transfer-Encoding") != null;
        if (declarado > maxBytes || sinLongitud) {
            log.warn("Cuerpo rechazado en {} {}: content-length={}, máximo={}", request.getMethod(),
                    LogSeguro.limpiar(request.getRequestURI()), declarado, maxBytes);
            escritor.escribir(response, HttpStatus.PAYLOAD_TOO_LARGE, CODIGO,
                    "El cuerpo de la petición supera el tamaño máximo permitido (" + maxBytes + " bytes)",
                    request.getRequestURI());
            return;
        }
        chain.doFilter(request, response);
    }
}
