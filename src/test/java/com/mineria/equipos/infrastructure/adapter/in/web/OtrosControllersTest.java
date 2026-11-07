package com.mineria.equipos.infrastructure.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.mineria.equipos.application.port.in.AutenticarUsuarioUseCase;
import com.mineria.equipos.application.view.SesionView;
import com.mineria.equipos.shared.exception.CredencialesInvalidasException;
import com.mineria.equipos.shared.exception.EquipoNotFoundException;
import com.mineria.equipos.domain.model.Rol;
import com.mineria.equipos.testsupport.ControllerTest;
import com.mineria.equipos.testsupport.Datos;
import com.mineria.equipos.testsupport.Tokens;

/** Controller de login. */
@ControllerTest(AuthController.class)
class OtrosControllersTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AutenticarUsuarioUseCase autenticarUsuario;

    private String token() {
        return Tokens.bearer(Tokens.valido("admin", Rol.ADMIN));
    }

    // ---------- /auth/login ----------

    @Test
    @DisplayName("POST /auth/login: 200 con el token, sin necesidad de autenticación")
    void loginCorrecto() throws Exception {
        // Arrange
        when(autenticarUsuario.autenticar(any()))
                .thenReturn(SesionView.builder().accessToken("jwt-de-prueba").tokenType("Bearer").expiresIn(3600).build());

        // Act + Assert
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"clave\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt-de-prueba"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600));
    }

    @Test
    @DisplayName("POST /auth/login: 401 con el formato común si las credenciales son incorrectas")
    void loginIncorrecto() throws Exception {
        // Arrange
        when(autenticarUsuario.autenticar(any())).thenThrow(new CredencialesInvalidasException());

        // Act + Assert
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"mala\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("CREDENCIALES_INVALIDAS"))
                .andExpect(jsonPath("$.path").value("/auth/login"));
    }

    @Test
    @DisplayName("POST /auth/login: 400 si username o password están vacíos, en blanco o ausentes")
    void loginInvalido() throws Exception {
        // Act + Assert
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"password\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[?(@.field=='username')]").isNotEmpty())
                .andExpect(jsonPath("$.details[?(@.field=='password')]").isNotEmpty());
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDACION_FALLIDA"));
    }

    // ---------- /equipos/{id}/mantenimientos ----------
}
