package com.mineria.equipos.shared.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.method.MethodValidationResult;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import com.mineria.equipos.auth.entity.Rol;
import com.mineria.equipos.testsupport.ControllerTest;
import com.mineria.equipos.testsupport.ErroresDePruebaController;
import com.mineria.equipos.testsupport.Tokens;


/** Prueba cada rama del GlobalExceptionHandler lanzando excepciones desde un controller de prueba. */
@ControllerTest(ErroresDePruebaController.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private GlobalExceptionHandler handler;

    private String token() {
        return Tokens.bearer(Tokens.valido("admin", Rol.ADMIN));
    }

    @Test
    @DisplayName("DataIntegrityViolationException: 409 sin filtrar detalles de SQL")
    void integridadDeDatos() throws Exception {
        // Act + Assert
        mockMvc.perform(get("/pruebas/integridad").header(HttpHeaders.AUTHORIZATION, token()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICTO_DE_DATOS"))
                .andExpect(content().string(not(containsString("fk_secreta"))))
                .andExpect(content().string(not(containsString("detalle interno"))));
    }

    @Test
    @DisplayName("AccessDeniedException lanzada en un controller: 403")
    void accesoDenegado() throws Exception {
        // Act + Assert
        mockMvc.perform(get("/pruebas/acceso-denegado").header(HttpHeaders.AUTHORIZATION, token()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESO_DENEGADO"));
    }

    @Test
    @DisplayName("AuthenticationException lanzada en un controller: 401")
    void autenticacion() throws Exception {
        // Act + Assert
        mockMvc.perform(get("/pruebas/autenticacion").header(HttpHeaders.AUTHORIZATION, token()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("NO_AUTENTICADO"));
    }

    @Test
    @DisplayName("Exception genérica: 500 sin filtrar el mensaje interno ni la traza")
    void errorInesperado() throws Exception {
        // Act + Assert
        mockMvc.perform(get("/pruebas/inesperado").header(HttpHeaders.AUTHORIZATION, token()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.code").value("ERROR_INTERNO"))
                .andExpect(content().string(not(containsString("secreto-123"))))
                .andExpect(content().string(not(containsString("IllegalStateException"))))
                .andExpect(content().string(not(containsString("at com.mineria"))));
    }

    @Test
    @DisplayName("ConstraintViolationException: 400 con el nombre del parámetro")
    void restriccionesDeParametros() throws Exception {
        // Act + Assert
        mockMvc.perform(get("/pruebas/restricciones").header(HttpHeaders.AUTHORIZATION, token()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDACION_FALLIDA"))
                .andExpect(jsonPath("$.details[0].field").value("id"))
                .andExpect(jsonPath("$.details[0].reason").value("debe ser mayor que o igual a 1"));
    }

    @Test
    @DisplayName("ruta inexistente: 404 con el formato común")
    void rutaInexistente() throws Exception {
        // Act + Assert
        mockMvc.perform(get("/no-existe").header(HttpHeaders.AUTHORIZATION, token()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RUTA_NO_ENCONTRADA"))
                .andExpect(jsonPath("$.path").value("/no-existe"));
    }

    @Test
    @DisplayName("método HTTP no permitido: 405 con el formato común")
    void metodoNoPermitido() throws Exception {
        // Act + Assert
        mockMvc.perform(get("/pruebas/solo-json").header(HttpHeaders.AUTHORIZATION, token()))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("METODO_NO_PERMITIDO"));
    }

    @Test
    @DisplayName("tipo de contenido no soportado: 415 con el formato común")
    void mediaTypeNoSoportado() throws Exception {
        // Act + Assert
        mockMvc.perform(post("/pruebas/solo-json").header(HttpHeaders.AUTHORIZATION, token())
                        .contentType(MediaType.APPLICATION_XML).content("<a/>"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("MEDIA_TYPE_NO_SOPORTADO"));
    }

    @Test
    @DisplayName("el error siempre sale en JSON, aunque el cliente pida XML")
    void erroresSiempreEnJson() throws Exception {
        // Act + Assert
        mockMvc.perform(get("/pruebas/inesperado").header(HttpHeaders.AUTHORIZATION, token())
                        .accept(MediaType.APPLICATION_XML))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("HandlerMethodValidationException: 400 con un detalle por parámetro")
    void validacionDeMetodo() {
        // Arrange: un resultado de validación que dice que el parámetro "id" no cumple
        MethodParameter parametro = mock(MethodParameter.class);
        when(parametro.getParameterName()).thenReturn("id");
        ParameterValidationResult resultado = mock(ParameterValidationResult.class);
        when(resultado.getMethodParameter()).thenReturn(parametro);
        when(resultado.getResolvableErrors()).thenReturn(List.of(new DefaultMessageSourceResolvable(
                new String[] {"Min"}, null, "debe ser mayor que o igual a 1")));
        MethodValidationResult validacion = mock(MethodValidationResult.class);
        when(validacion.getParameterValidationResults()).thenReturn(List.of(resultado));
        when(validacion.getAllErrors()).thenReturn(List.of());
        HandlerMethodValidationException excepcion = new HandlerMethodValidationException(validacion);
        MockHttpServletRequest peticion = new MockHttpServletRequest("GET", "/equipos/0");

        // Act
        ResponseEntity<Object> respuesta = handler.handleHandlerMethodValidationException(
                excepcion, new HttpHeaders(), HttpStatus.BAD_REQUEST, new ServletWebRequest(peticion));

        // Assert
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(String.valueOf(respuesta.getBody())).contains("VALIDACION_FALLIDA", "id", "debe ser mayor que o igual a 1");
    }

    @Test
    @DisplayName("Accept incompatible: 406 con el formato común")
    void formatoNoDisponible() throws Exception {
        // Act + Assert
        mockMvc.perform(get("/pruebas/salida-json").header(HttpHeaders.AUTHORIZATION, token())
                        .accept(MediaType.APPLICATION_XML))
                .andExpect(status().isNotAcceptable())
                .andExpect(jsonPath("$.code").value("FORMATO_NO_DISPONIBLE"));
    }

    @Test
    @DisplayName("falta un parámetro obligatorio: 400 genérico PETICION_INVALIDA")
    void parametroObligatorioAusente() throws Exception {
        // Act + Assert
        mockMvc.perform(get("/pruebas/con-parametro").header(HttpHeaders.AUTHORIZATION, token()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PETICION_INVALIDA"));
    }

    @Test
    @DisplayName("JSON con otra forma (un array donde va un objeto): 400 JSON_INVALIDO sin campo")
    void jsonConOtraForma() throws Exception {
        // Act + Assert
        mockMvc.perform(post("/pruebas/mapa").header(HttpHeaders.AUTHORIZATION, token())
                        .contentType(MediaType.APPLICATION_JSON).content("[1,2]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("JSON_INVALIDO"))
                .andExpect(jsonPath("$.details").isEmpty());
    }

    @Test
    @DisplayName("un error 5xx de Spring MVC sale con el código ERROR_INTERNO y mensaje genérico")
    void errorDeServidorDeSpring() {
        // Arrange
        MockHttpServletRequest peticion = new MockHttpServletRequest("GET", "/algo");

        // Act
        ResponseEntity<Object> respuesta = handler.handleExceptionInternal(new IllegalStateException("interno"), null,
                new HttpHeaders(), HttpStatus.SERVICE_UNAVAILABLE, new ServletWebRequest(peticion));

        // Assert
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(String.valueOf(respuesta.getBody())).contains("ERROR_INTERNO", "Ocurrió un error inesperado")
                .doesNotContain("interno");
    }

    @Test
    @DisplayName("errores globales de validación (no atados a un campo) también salen en details")
    void erroresGlobalesDeValidacion() throws Exception {
        // Arrange
        BeanPropertyBindingResult resultado = new BeanPropertyBindingResult(new Object(), "equipoRequest");
        resultado.reject("FechasIncoherentes", "las fechas no son coherentes");
        MethodParameter parametro = new MethodParameter(Object.class.getMethod("toString"), -1);
        MethodArgumentNotValidException excepcion = new MethodArgumentNotValidException(parametro, resultado);
        MockHttpServletRequest peticion = new MockHttpServletRequest("POST", "/equipos");

        // Act
        ResponseEntity<Object> respuesta = handler.handleMethodArgumentNotValid(
                excepcion, new HttpHeaders(), HttpStatus.BAD_REQUEST, new ServletWebRequest(peticion));

        // Assert
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(String.valueOf(respuesta.getBody())).contains("equipoRequest", "las fechas no son coherentes");
    }
}
