package com.mineria.equipos.tipo.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.mineria.equipos.auth.entity.Rol;
import com.mineria.equipos.generated.model.TipoEquipoResponse;
import com.mineria.equipos.testsupport.ControllerTest;
import com.mineria.equipos.testsupport.Tokens;
import com.mineria.equipos.tipo.service.TipoService;

@ControllerTest(TipoController.class)
class TipoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TipoService tipoService;

    @Test
    @DisplayName("GET /tipos: 200 con el catálogo")
    void tipos() throws Exception {
        // Arrange
        when(tipoService.listar()).thenReturn(List.of(
                new TipoEquipoResponse(1L, "Camión", 250),
                new TipoEquipoResponse(2L, "Excavadora", 500)));

        // Act + Assert
        mockMvc.perform(get("/tipos").header(HttpHeaders.AUTHORIZATION, Tokens.bearer(Tokens.valido("admin", Rol.ADMIN))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].intervaloHoras").value(500));
    }

    @Test
    @DisplayName("GET /tipos: 401 sin token")
    void tiposSinToken() throws Exception {
        // Act + Assert
        mockMvc.perform(get("/tipos")).andExpect(status().isUnauthorized());
    }
}
