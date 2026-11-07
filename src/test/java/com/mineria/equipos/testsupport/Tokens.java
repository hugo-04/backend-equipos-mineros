package com.mineria.equipos.testsupport;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import com.mineria.equipos.domain.model.Rol;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/** Fabrica tokens JWT para las pruebas con el mismo secreto de application-test.yml. */
public final class Tokens {

    public static final String SECRETO = "secreto-solo-para-pruebas-de-al-menos-32-caracteres";

    private Tokens() {
    }

    /** Token válido por 30 minutos para el usuario y rol dados. */
    public static String valido(String username, Rol rol) {
        return firmar(username, rol.name(), Instant.now().plusSeconds(1800), SECRETO);
    }

    /** Token firmado correctamente pero que ya venció. */
    public static String expirado(String username, Rol rol) {
        return firmar(username, rol.name(), Instant.now().minusSeconds(60), SECRETO);
    }

    /** Token con la estructura correcta pero firmado con otro secreto. */
    public static String conFirmaAjena(String username, Rol rol) {
        return firmar(username, rol.name(), Instant.now().plusSeconds(1800),
                "otro-secreto-distinto-para-simular-una-firma-falsa-123");
    }

    public static String firmar(String username, String rol, Instant expira, String secreto) {
        SecretKey clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(username)
                .claim("rol", rol)
                .issuedAt(Date.from(expira.minusSeconds(3600)))
                .expiration(Date.from(expira))
                .signWith(clave)
                .compact();
    }

    public static String bearer(String token) {
        return "Bearer " + token;
    }
}
