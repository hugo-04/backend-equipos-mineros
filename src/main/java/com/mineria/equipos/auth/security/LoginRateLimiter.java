package com.mineria.equipos.auth.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.stereotype.Component;

import com.mineria.equipos.shared.config.SecurityProperties;

/**
 * Cuenta los logins fallidos por cliente (IP) dentro de una ventana de tiempo para frenar los ataques de
 * fuerza bruta. Cuando se alcanza el máximo, el cliente queda bloqueado hasta que su fallo más viejo salga de la
 * ventana. Un login correcto borra el conteo. Vive en memoria: con varias instancias cada una cuenta por su lado
 * (es una defensa de mejor esfuerzo, y el backend de este proyecto corre en una sola instancia).
 */
@Component
public class LoginRateLimiter {

    /** A partir de aquí se limpian las entradas vencidas, para que el mapa no crezca sin límite. */
    private static final int LIMITE_DE_ENTRADAS = 10_000;

    private final int maxIntentos;
    private final Duration ventana;
    private final Clock clock;
    private final ConcurrentMap<String, Deque<Instant>> fallos = new ConcurrentHashMap<>();

    public LoginRateLimiter(SecurityProperties propiedades, Clock clock) {
        this.maxIntentos = propiedades.loginMaxIntentos();
        this.ventana = Duration.ofSeconds(propiedades.loginVentanaSegundos());
        this.clock = clock;
    }

    public boolean estaBloqueado(String cliente) {
        Deque<Instant> recientes = fallos.compute(cliente, (k, actuales) -> sinVencidos(actuales));
        return recientes != null && recientes.size() >= maxIntentos;
    }

    public void registrarFallo(String cliente) {
        if (fallos.size() >= LIMITE_DE_ENTRADAS) {
            // computeIfPresent elimina la entrada cuando ya no quedan fallos vigentes (devuelve null)
            fallos.keySet().forEach(k -> fallos.computeIfPresent(k, (clave, actuales) -> sinVencidos(actuales)));
        }
        fallos.compute(cliente, (k, actuales) -> {
            Deque<Instant> recientes = sinVencidos(actuales);
            Deque<Instant> resultado = recientes != null ? recientes : new ArrayDeque<>();
            resultado.addLast(clock.instant());
            return resultado;
        });
    }

    public void registrarExito(String cliente) {
        fallos.remove(cliente);
    }

    /** Segundos que faltan para que se libere el bloqueo (mínimo 1). */
    public long segundosParaReintentar(String cliente) {
        Deque<Instant> recientes = fallos.get(cliente);
        if (recientes == null || recientes.isEmpty()) {
            return 1;
        }
        long restante = Duration.between(clock.instant(), recientes.peekFirst().plus(ventana)).toSeconds();
        return Math.max(1, restante + 1);
    }

    private Deque<Instant> sinVencidos(Deque<Instant> actuales) {
        if (actuales == null) {
            return null;
        }
        Instant limite = clock.instant().minus(ventana);
        while (!actuales.isEmpty() && !actuales.peekFirst().isAfter(limite)) {
            actuales.pollFirst();
        }
        return actuales.isEmpty() ? null : actuales;
    }
}
