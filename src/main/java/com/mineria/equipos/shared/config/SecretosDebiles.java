package com.mineria.equipos.shared.config;

import java.util.List;
import java.util.Locale;

/**
 * Dice si un secreto o una contraseña es de los que se copian de una plantilla o de los que todo el mundo
 * adivina. La app lo usa para negarse a arrancar con eso.
 */
public final class SecretosDebiles {

    /** Palabras que delatan un valor de ejemplo o una clave previsible. */
    private static final List<String> PALABRAS_PROHIBIDAS = List.of(
            "cambia", "changeme", "change-me", "password", "contrasena", "contraseña", "ejemplo", "example",
            "default", "admin123", "123456", "qwerty", "letmein", "secret123", "xxxxxxxx");

    /** Con menos caracteres distintos que esto el secreto es demasiado repetitivo (tipo "aaaa..."). */
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
