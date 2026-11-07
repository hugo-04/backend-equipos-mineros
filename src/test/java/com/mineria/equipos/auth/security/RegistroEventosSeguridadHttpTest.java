package com.mineria.equipos.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.mineria.equipos.auth.controller.AuthController;
import com.mineria.equipos.auth.entity.Rol;
import com.mineria.equipos.auth.service.AuthService;
import com.mineria.equipos.equipo.controller.EquipoController;
import com.mineria.equipos.equipo.service.EquipoService;
import com.mineria.equipos.shared.exception.CredencialesInvalidasException;
import com.mineria.equipos.testsupport.ControllerTest;
import com.mineria.equipos.testsupport.Tokens;

/**
 * A09: los eventos de seguridad (401, 403, login rechazado) quedan en el log con el traceId de la petición,
 * y nunca se escribe el token ni la contraseña.
 */
@ExtendWith(OutputCaptureExtension.class)
@ControllerTest({EquipoController.class, AuthController.class})
class RegistroEventosSeguridadHttpTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EquipoService equipoService;
    @MockitoBean
    private AuthService authService;

    @Test
    @DisplayName("un 401 sin token se registra con método, ruta, motivo y el mismo traceId que ve el cliente")
    void registra401(CapturedOutput salida) throws Exception {
        // Act
        MvcResult resultado = mockMvc.perform(get("/equipos")).andExpect(status().isUnauthorized()).andReturn();
        String traceId = resultado.getResponse().getHeader("X-Trace-Id");

        // Assert
        assertThat(salida.getAll()).contains("Seguridad: acceso no autenticado GET /equipos motivo=NO_AUTENTICADO");
        assertThat(salida.getAll()).contains("traceId=" + traceId);
    }

    @Test
    @DisplayName("un 401 por token inválido se registra con su motivo y SIN escribir el token")
    void registra401PorTokenInvalido(CapturedOutput salida) throws Exception {
        // Arrange
        String tokenSecreto = "eyJhbGciOiJIUzI1NiJ9.contenido-secreto-del-token.firma-secreta";

        // Act
        mockMvc.perform(get("/equipos").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSecreto))
                .andExpect(status().isUnauthorized());

        // Assert
        assertThat(salida.getAll()).contains("motivo=TOKEN_INVALIDO");
        assertThat(salida.getAll()).doesNotContain(tokenSecreto).doesNotContain("contenido-secreto-del-token");
    }

    @Test
    @DisplayName("un 403 se registra con el usuario que intentó la acción")
    void registra403(CapturedOutput salida) throws Exception {
        // Arrange
        String operador = Tokens.bearer(Tokens.valido("operador-juan", Rol.OPERADOR));

        // Act
        mockMvc.perform(delete("/equipos/4").header(HttpHeaders.AUTHORIZATION, operador)).andExpect(status().isForbidden());

        // Assert
        assertThat(salida.getAll()).contains("Seguridad: acceso denegado DELETE /equipos/4 usuario=operador-juan");
    }

    @Test
    @DisplayName("el traceId del error del cliente coincide con la cabecera y con el log")
    void erroresTraenElTraceId(CapturedOutput salida) throws Exception {
        // Act
        MvcResult resultado = mockMvc.perform(get("/equipos").header("X-Trace-Id", "front-9999-trace"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.traceId").value("front-9999-trace"))
                .andReturn();

        // Assert
        assertThat(resultado.getResponse().getHeader("X-Trace-Id")).isEqualTo("front-9999-trace");
        assertThat(salida.getAll()).contains("traceId=front-9999-trace");
    }

    @Test
    @DisplayName("un username con saltos de línea ni siquiera llega al servicio: el contrato lo rechaza con 400")
    void usernameConSaltosDeLineaSeRechaza(CapturedOutput salida) throws Exception {
        // Arrange
        String cuerpo = "{\"username\":\"admin\r\nINFO falso: login correcto\",\"password\":\"clave-super-secreta\"}";

        // Act
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isBadRequest());

        // Assert: no se llamó al servicio y la contraseña no aparece en ningún log
        verify(authService, never()).login(any());
        assertThat(salida.getAll()).doesNotContain("clave-super-secreta");
    }

    @Test
    @DisplayName("un login rechazado se registra sin la contraseña")
    void loginRechazadoNoFiltraLaPassword(CapturedOutput salida) throws Exception {
        // Arrange
        doThrow(new CredencialesInvalidasException()).when(authService).login(any());

        // Act
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"clave-super-secreta\"}"))
                .andExpect(status().isUnauthorized());

        // Assert
        assertThat(salida.getAll()).doesNotContain("clave-super-secreta");
    }
}
