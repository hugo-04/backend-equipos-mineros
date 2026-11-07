package com.mineria.equipos.auth.security;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.mineria.equipos.auth.entity.Rol;
import com.mineria.equipos.generated.model.HealthResponse;
import com.mineria.equipos.shared.health.HealthController;
import com.mineria.equipos.shared.health.HealthService;
import com.mineria.equipos.testsupport.ControllerTest;
import com.mineria.equipos.testsupport.Tokens;
import com.mineria.equipos.tipo.controller.TipoController;
import com.mineria.equipos.tipo.service.TipoService;

/** Las cabeceras de seguridad deben salir en TODAS las respuestas: éxito, 401 y rutas inexistentes. */
@ControllerTest({TipoController.class, HealthController.class})
class CabecerasSeguridadHttpTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TipoService tipoService;
    @MockitoBean
    private HealthService healthService;

    private void afirmarCabeceras(ResultActions resultado) throws Exception {
        resultado
                .andExpect(header().string("Strict-Transport-Security", "max-age=31536000 ; includeSubDomains"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"))
                .andExpect(header().string("Content-Security-Policy", containsString("frame-ancestors 'none'")))
                .andExpect(header().string("Content-Security-Policy", containsString("default-src 'none'")))
                .andExpect(header().string("Permissions-Policy", containsString("camera=()")))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")));
    }

    @Test
    @DisplayName("una respuesta autenticada correcta lleva todas las cabeceras")
    void respuestaCorrecta() throws Exception {
        // Arrange
        when(tipoService.listar()).thenReturn(List.of());
        String token = Tokens.bearer(Tokens.valido("admin", Rol.ADMIN));

        // Act + Assert
        afirmarCabeceras(mockMvc.perform(get("/tipos").header(HttpHeaders.AUTHORIZATION, token)).andExpect(status().isOk()));
    }

    @Test
    @DisplayName("el 401 también lleva todas las cabeceras")
    void respuesta401() throws Exception {
        // Act + Assert
        afirmarCabeceras(mockMvc.perform(get("/tipos")).andExpect(status().isUnauthorized()));
    }

    @Test
    @DisplayName("el endpoint público /health también")
    void healthPublico() throws Exception {
        // Arrange
        when(healthService.verificar()).thenReturn(new HealthResponse().status(HealthResponse.StatusEnum.UP));

        // Act + Assert
        afirmarCabeceras(mockMvc.perform(get("/health")).andExpect(status().isOk()));
    }

    @Test
    @DisplayName("una ruta inexistente autenticada (404) también")
    void respuesta404() throws Exception {
        // Arrange
        String token = Tokens.bearer(Tokens.valido("admin", Rol.ADMIN));

        // Act + Assert
        afirmarCabeceras(mockMvc.perform(get("/no-existe").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNotFound()));
    }

    @Test
    @DisplayName("la API no revela tecnología: sin cabeceras Server ni X-Powered-By")
    void sinCabecerasQueDelatenLaTecnologia() throws Exception {
        // Act + Assert
        mockMvc.perform(get("/tipos"))
                .andExpect(header().doesNotExist("X-Powered-By"))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, not(containsString("html"))));
    }
}
