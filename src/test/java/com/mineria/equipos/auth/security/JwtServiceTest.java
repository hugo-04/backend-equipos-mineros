package com.mineria.equipos.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import com.mineria.equipos.auth.entity.Rol;
import com.mineria.equipos.shared.config.AppProperties;
import com.mineria.equipos.shared.exception.TokenJwtException;
import com.mineria.equipos.testsupport.Tokens;

class JwtServiceTest {

    private static final Instant INICIO = Instant.parse("2026-06-15T12:00:00Z");

    private AppProperties propiedades(String secreto) {
        return new AppProperties("test", new AppProperties.Jwt(secreto, 30),
                new AppProperties.Admin("a", "b"), new AppProperties.Cors(List.of("http://localhost:3000")));
    }

    private JwtService servicioEn(Instant momento) {
        return new JwtService(propiedades(Tokens.SECRETO), Clock.fixed(momento, ZoneOffset.UTC));
    }

    private UserDetails usuario(String username, String... roles) {
        return User.withUsername(username).password("hash").roles(roles).build();
    }

    @Test
    @DisplayName("generarToken: arma un JWT de tres partes y la duración configurada en segundos")
    void generar() {
        // Arrange
        JwtService servicio = servicioEn(INICIO);

        // Act
        String token = servicio.generarToken(usuario("admin", "ADMIN"));

        // Assert
        assertThat(token.split("\\.")).hasSize(3);
        assertThat(servicio.getExpiracionSegundos()).isEqualTo(30 * 60);
    }

    @Test
    @DisplayName("autenticar: un token recién emitido da el usuario y su rol")
    void tokenValido() {
        // Arrange
        JwtService servicio = servicioEn(INICIO);
        String token = servicio.generarToken(usuario("admin", "ADMIN"));

        // Act
        UsernamePasswordAuthenticationToken autenticacion = servicio.autenticar(token);

        // Assert
        assertThat(autenticacion.getName()).isEqualTo("admin");
        assertThat(autenticacion.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_ADMIN");
        assertThat(autenticacion.isAuthenticated()).isTrue();
    }

    @Test
    @DisplayName("autenticar: el rol OPERADOR se conserva en el token")
    void tokenDeOperador() {
        // Arrange
        JwtService servicio = servicioEn(INICIO);

        // Act
        UsernamePasswordAuthenticationToken autenticacion =
                servicio.autenticar(servicio.generarToken(usuario("op", "OPERADOR")));

        // Assert
        assertThat(autenticacion.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_OPERADOR");
    }

    @Test
    @DisplayName("generarToken: un usuario sin rol no puede recibir token")
    void usuarioSinRol() {
        // Arrange
        JwtService servicio = servicioEn(INICIO);
        UserDetails sinRol = User.withUsername("sin-rol").password("hash").authorities("PERMISO_SUELTO").build();

        // Act + Assert
        assertThatThrownBy(() -> servicio.generarToken(sinRol))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("sin-rol");
    }

    @Test
    @DisplayName("autenticar: un token vencido falla con TOKEN_EXPIRADO")
    void tokenExpirado() {
        // Arrange: se emite a las 12:00 con 30 min de vida y se valida a las 12:31
        String token = servicioEn(INICIO).generarToken(usuario("admin", "ADMIN"));
        JwtService masTarde = servicioEn(INICIO.plus(Duration.ofMinutes(31)));

        // Act + Assert
        assertThatThrownBy(() -> masTarde.autenticar(token))
                .isInstanceOf(TokenJwtException.class)
                .extracting(e -> ((TokenJwtException) e).getCodigo())
                .isEqualTo(TokenJwtException.TOKEN_EXPIRADO);
    }

    @Test
    @DisplayName("autenticar: justo antes de vencer todavía es válido")
    void tokenAunVigente() {
        // Arrange
        String token = servicioEn(INICIO).generarToken(usuario("admin", "ADMIN"));
        JwtService casiVencido = servicioEn(INICIO.plus(Duration.ofMinutes(29)));

        // Act + Assert
        assertThat(casiVencido.autenticar(token).getName()).isEqualTo("admin");
    }

    @Test
    @DisplayName("autenticar: texto que no es un JWT falla con TOKEN_INVALIDO")
    void tokenMalformado() {
        // Arrange
        JwtService servicio = servicioEn(INICIO);

        // Act + Assert
        assertThatThrownBy(() -> servicio.autenticar("esto-no-es-un-jwt"))
                .isInstanceOfSatisfying(TokenJwtException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo(TokenJwtException.TOKEN_INVALIDO));
    }

    @Test
    @DisplayName("autenticar: un token vacío falla con TOKEN_INVALIDO")
    void tokenVacio() {
        // Arrange
        JwtService servicio = servicioEn(INICIO);

        // Act + Assert
        assertThatThrownBy(() -> servicio.autenticar(""))
                .isInstanceOfSatisfying(TokenJwtException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo(TokenJwtException.TOKEN_INVALIDO));
    }

    @Test
    @DisplayName("autenticar: un token firmado con otro secreto falla con TOKEN_INVALIDO")
    void firmaFalsa() {
        // Arrange
        JwtService servicio = new JwtService(propiedades(Tokens.SECRETO), Clock.systemUTC());
        String falso = Tokens.conFirmaAjena("admin", Rol.ADMIN);

        // Act + Assert
        assertThatThrownBy(() -> servicio.autenticar(falso))
                .isInstanceOfSatisfying(TokenJwtException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo(TokenJwtException.TOKEN_INVALIDO));
    }

    @Test
    @DisplayName("autenticar: un rol que no existe falla con TOKEN_INVALIDO")
    void rolDesconocido() {
        // Arrange
        JwtService servicio = new JwtService(propiedades(Tokens.SECRETO), Clock.systemUTC());
        String token = Tokens.firmar("admin", "SUPERUSUARIO", Instant.now().plusSeconds(600), Tokens.SECRETO);

        // Act + Assert
        assertThatThrownBy(() -> servicio.autenticar(token))
                .isInstanceOfSatisfying(TokenJwtException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo(TokenJwtException.TOKEN_INVALIDO));
    }

    @Test
    @DisplayName("el token emitido lleva emisor, audiencia, usuario, rol y vence a los 30 minutos")
    void claimsDelTokenEmitido() throws Exception {
        // Arrange
        JwtService servicio = servicioEn(INICIO);

        // Act
        String token = servicio.generarToken(usuario("admin", "ADMIN"));
        String payload = new String(java.util.Base64.getUrlDecoder().decode(token.split("\\.")[1]),
                java.nio.charset.StandardCharsets.UTF_8);
        var claims = new com.fasterxml.jackson.databind.ObjectMapper().readTree(payload);

        // Assert
        assertThat(claims.get("iss").asText()).isEqualTo("equipos-mineros-backend");
        assertThat(claims.get("aud").toString()).contains("equipos-mineros-api");
        assertThat(claims.get("sub").asText()).isEqualTo("admin");
        assertThat(claims.get("rol").asText()).isEqualTo("ADMIN");
        assertThat(claims.get("exp").asLong() - claims.get("iat").asLong()).isEqualTo(30 * 60);
    }

    @Test
    @DisplayName("autenticar: un token sin emisor ni audiencia se rechaza")
    void sinEmisorNiAudiencia() {
        // Arrange
        JwtService servicio = new JwtService(propiedades(Tokens.SECRETO), Clock.systemUTC());
        String token = Tokens.firmar("admin", "ADMIN", Instant.now().plusSeconds(600), Tokens.SECRETO, null, null);

        // Act + Assert
        assertThatThrownBy(() -> servicio.autenticar(token))
                .isInstanceOfSatisfying(TokenJwtException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo(TokenJwtException.TOKEN_INVALIDO));
    }

    @Test
    @DisplayName("autenticar: un token de otro emisor se rechaza")
    void emisorIncorrecto() {
        // Arrange
        JwtService servicio = new JwtService(propiedades(Tokens.SECRETO), Clock.systemUTC());
        String token = Tokens.firmar("admin", "ADMIN", Instant.now().plusSeconds(600), Tokens.SECRETO,
                "otro-sistema", Tokens.AUDIENCIA);

        // Act + Assert
        assertThatThrownBy(() -> servicio.autenticar(token))
                .isInstanceOfSatisfying(TokenJwtException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo(TokenJwtException.TOKEN_INVALIDO));
    }

    @Test
    @DisplayName("autenticar: un token para otra audiencia se rechaza")
    void audienciaIncorrecta() {
        // Arrange
        JwtService servicio = new JwtService(propiedades(Tokens.SECRETO), Clock.systemUTC());
        String token = Tokens.firmar("admin", "ADMIN", Instant.now().plusSeconds(600), Tokens.SECRETO,
                Tokens.EMISOR, "otra-api");

        // Act + Assert
        assertThatThrownBy(() -> servicio.autenticar(token))
                .isInstanceOfSatisfying(TokenJwtException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo(TokenJwtException.TOKEN_INVALIDO));
    }

    @Test
    @DisplayName("autenticar: un token con alg none (sin firma) se rechaza")
    void algNone() {
        // Arrange: cabecera {"alg":"none"} y claims válidos, sin firma
        JwtService servicio = new JwtService(propiedades(Tokens.SECRETO), Clock.systemUTC());
        var b64 = java.util.Base64.getUrlEncoder().withoutPadding();
        String cabecera = b64.encodeToString("{\"alg\":\"none\",\"typ\":\"JWT\"}".getBytes());
        String claims = b64.encodeToString(("{\"iss\":\"" + Tokens.EMISOR + "\",\"aud\":\"" + Tokens.AUDIENCIA
                + "\",\"sub\":\"admin\",\"rol\":\"ADMIN\",\"exp\":" + (Instant.now().getEpochSecond() + 600) + "}").getBytes());

        // Act + Assert
        assertThatThrownBy(() -> servicio.autenticar(cabecera + "." + claims + "."))
                .isInstanceOfSatisfying(TokenJwtException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo(TokenJwtException.TOKEN_INVALIDO));
    }

    @Test
    @DisplayName("autenticar: un rol con el tipo equivocado (número) se rechaza sin romperse")
    void rolConTipoEquivocado() {
        // Arrange
        JwtService servicio = new JwtService(propiedades(Tokens.SECRETO), Clock.systemUTC());
        var clave = io.jsonwebtoken.security.Keys.hmacShaKeyFor(Tokens.SECRETO.getBytes());
        String token = io.jsonwebtoken.Jwts.builder().issuer(Tokens.EMISOR).audience().add(Tokens.AUDIENCIA).and()
                .subject("admin").claim("rol", 5).expiration(java.util.Date.from(Instant.now().plusSeconds(600)))
                .signWith(clave).compact();

        // Act + Assert
        assertThatThrownBy(() -> servicio.autenticar(token))
                .isInstanceOfSatisfying(TokenJwtException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo(TokenJwtException.TOKEN_INVALIDO));
    }
}
