package com.mineria.equipos.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import com.mineria.equipos.shared.exception.TokenJwtException;

/**
 * El filtro del JWT: con un token bueno deja autenticada la petición; con uno malo anota el motivo y deja
 * seguir, para que el entry point conteste el 401. Si algo inesperado falla al validar, la petición queda sin autenticar.
 */
class JwtAuthenticationFilterTest {

    private final JwtService jwtService = mock(JwtService.class);
    private final JwtAuthenticationFilter filtro = new JwtAuthenticationFilter(jwtService);

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("falla cerrado: un error inesperado al validar el token deja la petición sin autenticar y sigue la cadena")
    void falloInesperadoNoAutentica() throws Exception {
        // Arrange
        when(jwtService.autenticar("raro")).thenThrow(new IllegalStateException("fallo interno"));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/equipos");
        request.addHeader("Authorization", "Bearer raro");
        MockFilterChain cadena = new MockFilterChain();

        // Act
        filtro.doFilter(request, new MockHttpServletResponse(), cadena);

        // Assert: sin autenticación, con el motivo marcado para que el entry point conteste 401, y la cadena continuó
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(request.getAttribute(JwtAuthenticationFilter.ATRIBUTO_ERROR_TOKEN))
                .isEqualTo(TokenJwtException.TOKEN_INVALIDO);
        assertThat(cadena.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("sin header Authorization no consulta el servicio JWT")
    void sinHeader() throws Exception {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/equipos");

        // Act
        filtro.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        // Assert
        verify(jwtService, never()).autenticar(anyString());
        assertThat(request.getAttribute(JwtAuthenticationFilter.ATRIBUTO_ERROR_TOKEN)).isNull();
    }
}
