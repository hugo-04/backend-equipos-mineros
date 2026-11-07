package com.mineria.equipos.tipo.controller;

import static org.mockito.Mockito.when;
import com.mineria.equipos.generated.model.PaginaTipoEquipoResponse;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.ParameterizedTest;
import org.springframework.data.domain.PageRequest;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;
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

/**
 * El endpoint del catálogo de tipos por HTTP, con el servicio simulado.
 */
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
        when(tipoService.listar(any())).thenReturn(new PaginaTipoEquipoResponse(0, 20, 2L, 1, true, true, List.of(
                new TipoEquipoResponse(1L, "Camión", 250),
                new TipoEquipoResponse(2L, "Excavadora", 500))));

        // Act + Assert
        mockMvc.perform(get("/tipos").header(HttpHeaders.AUTHORIZATION, Tokens.bearer(Tokens.valido("admin", Rol.ADMIN))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[1].intervaloHoras").value(500))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    @DisplayName("GET /tipos sin page ni size usa la primera página de 20")
    void valoresPorDefecto() throws Exception {
        // Arrange
        when(tipoService.listar(any())).thenReturn(new PaginaTipoEquipoResponse());

        // Act
        mockMvc.perform(get("/tipos").header(HttpHeaders.AUTHORIZATION, Tokens.bearer(Tokens.valido("admin", Rol.ADMIN))))
                .andExpect(status().isOk());

        // Assert
        verify(tipoService).listar(PageRequest.of(0, 20));
    }

    @Test
    @DisplayName("GET /tipos?page=1&size=5 pide exactamente esa página")
    void pasaPageYSize() throws Exception {
        // Arrange
        when(tipoService.listar(any())).thenReturn(new PaginaTipoEquipoResponse());

        // Act
        mockMvc.perform(get("/tipos?page=1&size=5").header(HttpHeaders.AUTHORIZATION, Tokens.bearer(Tokens.valido("admin", Rol.ADMIN))))
                .andExpect(status().isOk());

        // Assert
        verify(tipoService).listar(PageRequest.of(1, 5));
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"page=-1", "size=0", "size=101", "size=-5"})
    @DisplayName("GET /tipos con paginación fuera de rango: 400 con el detalle del parámetro")
    void paginacionInvalida(String consulta) throws Exception {
        // Act + Assert
        mockMvc.perform(get("/tipos?" + consulta).header(HttpHeaders.AUTHORIZATION, Tokens.bearer(Tokens.valido("admin", Rol.ADMIN))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDACION_FALLIDA"));
        verify(tipoService, never()).listar(any());
    }

    @Test
    @DisplayName("GET /tipos?page=abc: 400 PARAMETRO_INVALIDO")
    void paginaNoNumerica() throws Exception {
        // Act + Assert
        mockMvc.perform(get("/tipos?page=abc").header(HttpHeaders.AUTHORIZATION, Tokens.bearer(Tokens.valido("admin", Rol.ADMIN))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PARAMETRO_INVALIDO"));
    }

    @Test
    @DisplayName("GET /tipos: 401 sin token")
    void tiposSinToken() throws Exception {
        // Act + Assert
        mockMvc.perform(get("/tipos")).andExpect(status().isUnauthorized());
    }
}
