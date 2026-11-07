package com.mineria.equipos.shared.trace;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Le da a cada petición un id de correlación (traceId). Se pone en el MDC para que salga en TODOS los logs de esa
 * petición, se devuelve en la cabecera {@code X-Trace-Id} y lo incluyen los errores ({@code ErrorResponse.traceId}),
 * así un error que ve el usuario se puede rastrear en los logs. Va primero de todos los filtros (incluido Spring
 * Security) para que hasta los 401 lo tengan. Si el cliente (por ejemplo el frontend) manda un id propio válido se
 * reutiliza; cualquier cosa rara se descarta y se genera uno nuevo, para no meter basura en los logs.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    private static final Pattern ID_VALIDO = Pattern.compile("^[A-Za-z0-9-]{8,64}$");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String id = idDeEntradaOGenerado(request.getHeader(TraceId.CABECERA));
        MDC.put(TraceId.CLAVE_MDC, id);
        response.setHeader(TraceId.CABECERA, id);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(TraceId.CLAVE_MDC);
        }
    }

    private String idDeEntradaOGenerado(String entrante) {
        return entrante != null && ID_VALIDO.matcher(entrante).matches() ? entrante : UUID.randomUUID().toString();
    }
}
