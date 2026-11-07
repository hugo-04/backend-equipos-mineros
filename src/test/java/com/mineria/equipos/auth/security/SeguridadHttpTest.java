package com.mineria.equipos.auth.security;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.mineria.equipos.tipo.service.TipoService;
import com.mineria.equipos.generated.model.HealthResponse;
import com.mineria.equipos.auth.entity.Rol;
import com.mineria.equipos.shared.health.HealthController;
import com.mineria.equipos.shared.health.HealthService;
import com.mineria.equipos.tipo.controller.TipoController;
import com.mineria.equipos.testsupport.ControllerTest;
import com.mineria.equipos.testsupport.Tokens;

/**
 * Seguridad de punta a punta sobre la cadena real de filtros: token válido, expirado, malformado,
 * con firma falsa o ausente, endpoints públicos y CORS.
 */
@ControllerTest({TipoController.class, HealthController.class})
class SeguridadHttpTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TipoService tipoService;
    @MockitoBean
    private HealthService healthService;

    @Test
    @DisplayName("token válido: pasa")
    void tokenValido() throws Exception {
        // Arrange
        when(tipoService.listar()).thenReturn(List.of());

        // Act + Assert
        mockMvc.perform(get("/tipos").header(HttpHeaders.AUTHORIZATION, Tokens.bearer(Tokens.valido("admin", Rol.ADMIN))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("token expirado: 401 TOKEN_EXPIRADO")
    void tokenExpirado() throws Exception {
        // Act + Assert
        mockMvc.perform(get("/tipos").header(HttpHeaders.AUTHORIZATION, Tokens.bearer(Tokens.expirado("admin", Rol.ADMIN))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_EXPIRADO"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("token malformado: 401 TOKEN_INVALIDO")
    void tokenMalformado() throws Exception {
        // Act + Assert
        mockMvc.perform(get("/tipos").header(HttpHeaders.AUTHORIZATION, "Bearer esto.no.sirve"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_INVALIDO"));
    }

    @Test
    @DisplayName("Bearer sin token: 401 TOKEN_INVALIDO")
    void bearerVacio() throws Exception {
        // Act + Assert
        mockMvc.perform(get("/tipos").header(HttpHeaders.AUTHORIZATION, "Bearer "))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_INVALIDO"));
    }

    @Test
    @DisplayName("firma falsa: 401 TOKEN_INVALIDO")
    void firmaFalsa() throws Exception {
        // Act + Assert
        mockMvc.perform(get("/tipos").header(HttpHeaders.AUTHORIZATION, Tokens.bearer(Tokens.conFirmaAjena("admin", Rol.ADMIN))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_INVALIDO"));
    }

    @Test
    @DisplayName("sin header Authorization: 401 NO_AUTENTICADO")
    void sinToken() throws Exception {
        // Act + Assert
        mockMvc.perform(get("/tipos"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("NO_AUTENTICADO"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("esquema distinto de Bearer (Basic): 401 NO_AUTENTICADO")
    void esquemaBasic() throws Exception {
        // Act + Assert
        mockMvc.perform(get("/tipos").header(HttpHeaders.AUTHORIZATION, "Basic YWRtaW46Y2xhdmU="))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("NO_AUTENTICADO"));
    }

    @Test
    @DisplayName("un endpoint público responde aunque el token enviado esté vencido")
    void publicoConTokenVencido() throws Exception {
        // Arrange
        when(healthService.verificar()).thenReturn(new HealthResponse().status(HealthResponse.StatusEnum.UP));

        // Act + Assert
        mockMvc.perform(get("/health").header(HttpHeaders.AUTHORIZATION, Tokens.bearer(Tokens.expirado("admin", Rol.ADMIN))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("la documentación Swagger no exige token (no da 401)")
    void swaggerEsPublico() throws Exception {
        // Act + Assert: en este slice springdoc no está cargado, así que el resultado es 404 y no 401
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isNotFound());
        mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("CORS: un origen permitido recibe los headers de la política, incluido Authorization")
    void corsOrigenPermitido() throws Exception {
        // Act + Assert
        mockMvc.perform(options("/equipos")
                        .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET,POST,PUT,DELETE,OPTIONS"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "authorization, content-type"));
    }

    @Test
    @DisplayName("CORS: un origen no permitido es rechazado")
    void corsOrigenNoPermitido() throws Exception {
        // Act + Assert
        mockMvc.perform(options("/equipos")
                        .header(HttpHeaders.ORIGIN, "http://sitio-malicioso.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden());
    }
}
