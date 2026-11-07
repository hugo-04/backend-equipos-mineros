package com.mineria.equipos.auth.security;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
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
 * Frena la fuerza bruta contra {@code POST /auth/login}: si una IP acumuló demasiados logins fallidos
 * responde 429 con {@code Retry-After} sin llegar siquiera a revisar la contraseña. Cuenta como fallo una
 * respuesta 401 del login; un login correcto reinicia el conteo. La IP real llega correcta porque
 * Tomcat resuelve {@code X-Forwarded-For} solo desde proxies de confianza (server.forward-headers-strategy=native).
 */
@Slf4j
@RequiredArgsConstructor
public class LoginRateLimitFilter extends OncePerRequestFilter {

    static final String CODIGO = "DEMASIADOS_INTENTOS";
    private static final String RUTA_LOGIN = "/auth/login";

    private final LoginRateLimiter limitador;
    private final ErrorResponseWriter escritor;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !(HttpMethod.POST.matches(request.getMethod()) && RUTA_LOGIN.equals(request.getRequestURI()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String cliente = request.getRemoteAddr();
        if (limitador.estaBloqueado(cliente)) {
            log.warn("Login bloqueado por exceso de intentos fallidos desde {}", LogSeguro.limpiar(cliente));
            response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(limitador.segundosParaReintentar(cliente)));
            escritor.escribir(response, HttpStatus.TOO_MANY_REQUESTS, CODIGO,
                    "Demasiados intentos de inicio de sesión. Espera un momento e inténtalo de nuevo",
                    request.getRequestURI());
            return;
        }

        chain.doFilter(request, response);

        if (response.getStatus() == HttpStatus.UNAUTHORIZED.value()) {
            limitador.registrarFallo(cliente);
        } else if (HttpStatus.valueOf(response.getStatus()).is2xxSuccessful()) {
            limitador.registrarExito(cliente);
        }
    }
}
