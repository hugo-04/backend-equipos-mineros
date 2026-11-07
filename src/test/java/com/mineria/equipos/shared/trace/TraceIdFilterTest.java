package com.mineria.equipos.shared.trace;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * El id de correlación: se reutiliza el que mande el cliente si tiene pinta válida y, si no, se genera uno.
 * Sale en la cabecera de respuesta, vive en el MDC durante la petición y se limpia al terminar.
 */
class TraceIdFilterTest {

    private final TraceIdFilter filtro = new TraceIdFilter();

    private String ejecutar(String idEntrante, AtomicReference<String> idEnElMdc) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/equipos");
        if (idEntrante != null) {
            request.addHeader(TraceId.CABECERA, idEntrante);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        filtro.doFilter(request, response, new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) {
                idEnElMdc.set(TraceId.actual());
            }
        });
        return response.getHeader(TraceId.CABECERA);
    }

    @Test
    @DisplayName("sin id entrante genera un UUID, lo pone en el MDC durante la petición y en la cabecera de respuesta")
    void generaUnId() throws Exception {
        // Arrange
        AtomicReference<String> enElMdc = new AtomicReference<>();

        // Act
        String cabecera = ejecutar(null, enElMdc);

        // Assert
        assertThat(cabecera).matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");
        assertThat(enElMdc.get()).isEqualTo(cabecera);
    }

    @Test
    @DisplayName("reutiliza un id entrante válido (por ejemplo, el que manda el frontend)")
    void reutilizaElIdValido() throws Exception {
        // Arrange
        AtomicReference<String> enElMdc = new AtomicReference<>();

        // Act
        String cabecera = ejecutar("front-1234-abcd", enElMdc);

        // Assert
        assertThat(cabecera).isEqualTo("front-1234-abcd");
        assertThat(enElMdc.get()).isEqualTo("front-1234-abcd");
    }

    @ParameterizedTest(name = "[{index}] id entrante rechazado")
    @ValueSource(strings = {"corto", "con espacios y simbolos!!", "id\r\nX-Falso: 1", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", "<script>alert(1)</script>"})
    @DisplayName("un id entrante raro (corto, largo, con símbolos o saltos de línea) se descarta y se genera uno nuevo")
    void descartaIdsRaros(String entrante) throws Exception {
        // Arrange
        AtomicReference<String> enElMdc = new AtomicReference<>();

        // Act
        String cabecera = ejecutar(entrante, enElMdc);

        // Assert
        assertThat(cabecera).isNotEqualTo(entrante).matches("^[0-9a-f-]{36}$");
    }

    @Test
    @DisplayName("limpia el MDC al terminar la petición, aunque falle")
    void limpiaElMdc() throws Exception {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/equipos");

        // Act + Assert: la cadena lanza una excepción
        try {
            filtro.doFilter(request, new MockHttpServletResponse(), new MockFilterChain() {
                @Override
                public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) {
                    throw new IllegalStateException("fallo");
                }
            });
        } catch (IllegalStateException esperado) {
            // lo importante es el estado del MDC
        }
        assertThat(MDC.get(TraceId.CLAVE_MDC)).isNull();
        assertThat(TraceId.actual()).isNull();
    }
}
