package com.mineria.equipos.infrastructure.security;

import com.mineria.equipos.shared.exception.TokenJwtException;

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

import com.mineria.equipos.application.view.SesionView;
import com.mineria.equipos.domain.model.Rol;
import com.mineria.equipos.domain.model.Usuario;
import com.mineria.equipos.infrastructure.config.AppProperties;
import com.mineria.equipos.testsupport.Datos;
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

    @Test
    @DisplayName("emitir: devuelve un Bearer con la duración configurada en segundos")
    void emitir() {
        // Arrange
        JwtService servicio = servicioEn(INICIO);

        // Act
        SesionView sesion = servicio.emitir(Datos.admin());

        // Assert
        assertThat(sesion.getTokenType()).isEqualTo("Bearer");
        assertThat(sesion.getExpiresIn()).isEqualTo(30 * 60);
        assertThat(sesion.getAccessToken().split("\\.")).hasSize(3);
    }

    @Test
    @DisplayName("autenticar: un token recién emitido da el usuario y su rol")
    void tokenValido() {
        // Arrange
        JwtService servicio = servicioEn(INICIO);
        String token = servicio.emitir(Datos.admin()).getAccessToken();

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
        Usuario operador = Usuario.builder().username("op").passwordHash("x").rol(Rol.OPERADOR).build();

        // Act
        UsernamePasswordAuthenticationToken autenticacion = servicio.autenticar(servicio.emitir(operador).getAccessToken());

        // Assert
        assertThat(autenticacion.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_OPERADOR");
    }

    @Test
    @DisplayName("autenticar: un token vencido falla con TOKEN_EXPIRADO")
    void tokenExpirado() {
        // Arrange: se emite a las 12:00 con 30 min de vida y se valida a las 12:31
        String token = servicioEn(INICIO).emitir(Datos.admin()).getAccessToken();
        JwtService mastarde = servicioEn(INICIO.plus(Duration.ofMinutes(31)));

        // Act + Assert
        assertThatThrownBy(() -> mastarde.autenticar(token))
                .isInstanceOf(TokenJwtException.class)
                .extracting(e -> ((TokenJwtException) e).getCodigo())
                .isEqualTo(TokenJwtException.TOKEN_EXPIRADO);
    }

    @Test
    @DisplayName("autenticar: justo antes de vencer todavía es válido")
    void tokenAunVigente() {
        // Arrange
        String token = servicioEn(INICIO).emitir(Datos.admin()).getAccessToken();
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
}
