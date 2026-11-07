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
 * Le pone a cada petición un id (traceId). Va en el MDC, así sale en todos los logs de esa petición; vuelve
 * en la cabecera {@code X-Trace-Id} y en el {@code ErrorResponse}, de modo que un error que ve el usuario se
 * puede buscar en los logs. Corre antes que todos los demás filtros, incluido Spring Security, para que
 * hasta un 401 tenga id. Si el cliente manda uno propio y tiene pinta válida lo respeto; si trae algo raro
 * genero uno nuevo, para no llenar los logs de basura.
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
