package com.mineria.equipos.auth.security;

import com.mineria.equipos.shared.exception.TokenJwtException;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import javax.crypto.SecretKey;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import com.mineria.equipos.auth.entity.Rol;
import com.mineria.equipos.shared.config.AppProperties;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Crea los tokens JWT (firmados con HS256) cuando alguien inicia sesión y los valida cuando llegan de
 * vuelta, convirtiéndolos en la autenticación de Spring Security. El secreto y la duración salen del entorno.
 */
@Component
public class JwtService {

    private static final String CLAIM_ROL = "rol";
    private static final String PREFIJO_ROL = "ROLE_";
    /** Quién emite el token y para quién es. Un token de otro sistema, o sin estos datos, se rechaza. */
    static final String EMISOR = "equipos-mineros-backend";
    static final String AUDIENCIA = "equipos-mineros-api";

    private final SecretKey clave;
    private final Duration duracion;
    private final Clock clock;

    public JwtService(AppProperties properties, Clock clock) {
        this.clave = Keys.hmacShaKeyFor(properties.jwt().secret().getBytes(StandardCharsets.UTF_8));
        this.duracion = Duration.ofMinutes(properties.jwt().expirationMinutes());
        this.clock = clock;
    }

    /** Genera el token del usuario ya autenticado. El rol viaja dentro del token para no ir a la BD en cada petición. */
    public String generarToken(UserDetails usuario) {
        Instant ahora = clock.instant();
        return Jwts.builder()
                .issuer(EMISOR)
                .audience().add(AUDIENCIA).and()
                .subject(usuario.getUsername())
                .claim(CLAIM_ROL, rolDe(usuario))
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(duracion)))
                .signWith(clave)
                .compact();
    }

    public long getExpiracionSegundos() {
        return duracion.toSeconds();
    }

    /**
     * Revisa firma, expiración, emisor y audiencia, y con eso arma la autenticación (usuario y rol).
     *
     * @throws TokenJwtException si el token está vencido, mal formado o la firma no coincide
     */
    public UsernamePasswordAuthenticationToken autenticar(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(clave)
                    .requireIssuer(EMISOR)
                    .requireAudience(AUDIENCIA)
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Rol rol = Rol.valueOf(claims.get(CLAIM_ROL, String.class));
            return UsernamePasswordAuthenticationToken.authenticated(
                    claims.getSubject(), null, List.of(new SimpleGrantedAuthority(PREFIJO_ROL + rol.name())));
        } catch (ExpiredJwtException e) {
            throw new TokenJwtException(TokenJwtException.TOKEN_EXPIRADO, "El token expiró", e);
        } catch (RuntimeException e) {
            // Falla cerrado: token mal formado, sin firma (alg none), con firma, emisor o audiencia que no son,
            // vacío, con un rol que no existe, o cualquier otro fallo al leerlo. En todos esos casos el token no sirve.
            throw new TokenJwtException(TokenJwtException.TOKEN_INVALIDO, "El token no es válido", e);
        }
    }

    private String rolDe(UserDetails usuario) {
        return usuario.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(autoridad -> autoridad.startsWith(PREFIJO_ROL))
                .map(autoridad -> autoridad.substring(PREFIJO_ROL.length()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("El usuario " + usuario.getUsername() + " no tiene rol"));
    }
}
