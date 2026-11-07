package com.mineria.equipos.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.mineria.equipos.auth.service.AuthService;
import com.mineria.equipos.generated.model.LoginRequest;
import com.mineria.equipos.generated.model.LoginResponse;
import com.mineria.equipos.shared.exception.CredencialesInvalidasException;
import com.mineria.equipos.testsupport.ControllerTest;

@ControllerTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Test
    @DisplayName("POST /auth/login: 200 con el token, sin necesidad de autenticación")
    void loginCorrecto() throws Exception {
        // Arrange
        when(authService.login(any())).thenReturn(new LoginResponse("jwt-de-prueba", "Bearer", 3600L));

        // Act + Assert
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"clave\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt-de-prueba"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600));
        ArgumentCaptor<LoginRequest> request = ArgumentCaptor.forClass(LoginRequest.class);
        verify(authService).login(request.capture());
        assertThat(request.getValue().getUsername()).isEqualTo("admin");
    }

    @Test
    @DisplayName("POST /auth/login: 401 con el formato común si las credenciales son incorrectas")
    void loginIncorrecto() throws Exception {
        // Arrange
        when(authService.login(any())).thenThrow(new CredencialesInvalidasException());

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
}
