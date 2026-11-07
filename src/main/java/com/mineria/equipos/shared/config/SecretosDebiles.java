package com.mineria.equipos.shared.config;

import java.util.List;
import java.util.Locale;

/**
 * Detecta secretos y contraseñas "de ejemplo" o de poca variedad. Sirve para que la aplicación se niegue a
 * arrancar con los valores de plantilla (por ejemplo, copiados tal cual de un .env de ejemplo) en lugar de
 * quedar protegida por una clave que cualquiera puede adivinar.
 */
public final class SecretosDebiles {

    /** Palabras típicas de valores de ejemplo o claves previsibles. */
    private static final List<String> PALABRAS_PROHIBIDAS = List.of(
            "cambia", "changeme", "change-me", "password", "contrasena", "contraseña", "ejemplo", "example",
            "default", "admin123", "123456", "qwerty", "letmein", "secret123", "xxxxxxxx");

    /** Un secreto con menos caracteres distintos que esto es demasiado repetitivo (por ejemplo "aaaa…"). */
    private static final int MIN_CARACTERES_DISTINTOS = 6;

    private SecretosDebiles() {
    }

    public static boolean esDebil(String secreto) {
        if (secreto == null || secreto.isBlank()) {
            return true;
        }
        String minusculas = secreto.toLowerCase(Locale.ROOT);
        boolean traePalabraDeEjemplo = PALABRAS_PROHIBIDAS.stream().anyMatch(minusculas::contains);
        long distintos = secreto.chars().distinct().count();
        return traePalabraDeEjemplo || distintos < MIN_CARACTERES_DISTINTOS;
    }
}
