package com.mineria.equipos.mantenimiento.controller;

import static org.assertj.core.api.Assertions.assertThat;
import com.mineria.equipos.generated.model.PaginaMantenimientoResponse;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.ParameterizedTest;
import org.springframework.data.domain.PageRequest;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.mineria.equipos.auth.entity.Rol;
import com.mineria.equipos.generated.model.MantenimientoRequest;
import com.mineria.equipos.mantenimiento.service.MantenimientoService;
import com.mineria.equipos.shared.exception.EquipoNotFoundException;
import com.mineria.equipos.testsupport.ControllerTest;
import com.mineria.equipos.testsupport.Datos;
import com.mineria.equipos.testsupport.Tokens;

@ControllerTest(MantenimientoController.class)
class MantenimientoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MantenimientoService mantenimientoService;

    private String token() {
        return Tokens.bearer(Tokens.valido("admin", Rol.ADMIN));
    }

    @Test
    @DisplayName("POST /equipos/{id}/mantenimientos: 201 con el registro del historial")
    void registrarMantenimientoCreado() throws Exception {
        // Arrange
        when(mantenimientoService.registrar(eq(3L), any())).thenReturn(Datos.mantenimientoResponse(5L, 3L, "300.0"));

        // Act + Assert
        mockMvc.perform(post("/equipos/3/mantenimientos").header(HttpHeaders.AUTHORIZATION, token())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"notas\":\"Cambio de aceite\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.equipoId").value(3))
                .andExpect(jsonPath("$.horometroAlRegistrar").value(300.0));
        ArgumentCaptor<MantenimientoRequest> request = ArgumentCaptor.forClass(MantenimientoRequest.class);
        verify(mantenimientoService).registrar(eq(3L), request.capture());
        assertThat(request.getValue().getNotas()).isEqualTo("Cambio de aceite");
    }

    @Test
    @DisplayName("POST /equipos/{id}/mantenimientos: las notas son opcionales")
    void notasOpcionales() throws Exception {
        // Arrange
        when(mantenimientoService.registrar(eq(3L), any())).thenReturn(Datos.mantenimientoResponse(5L, 3L, "300.0"));

        // Act + Assert
        mockMvc.perform(post("/equipos/3/mantenimientos").header(HttpHeaders.AUTHORIZATION, token())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("POST /equipos/{id}/mantenimientos: 400 si las notas pasan de 500 caracteres")
    void notasMuyLargas() throws Exception {
        // Act + Assert
        mockMvc.perform(post("/equipos/3/mantenimientos").header(HttpHeaders.AUTHORIZATION, token())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"notas\":\"" + "x".repeat(501) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("notas"));
    }

    @Test
    @DisplayName("POST /equipos/{id}/mantenimientos: 404 si el equipo no existe")
    void registrarEnEquipoInexistente() throws Exception {
        // Arrange
        when(mantenimientoService.registrar(eq(3L), any())).thenThrow(new EquipoNotFoundException(3L));

        // Act + Assert
        mockMvc.perform(post("/equipos/3/mantenimientos").header(HttpHeaders.AUTHORIZATION, token())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /equipos/{id}/mantenimientos: 200 con el historial")
    void historial() throws Exception {
        // Arrange
        when(mantenimientoService.listarPorEquipo(eq(3L), any())).thenReturn(new PaginaMantenimientoResponse(
                0, 20, 2L, 1, true, true,
                List.of(Datos.mantenimientoResponse(2L, 3L, "500.0"), Datos.mantenimientoResponse(1L, 3L, "250.0"))));

        // Act + Assert
        mockMvc.perform(get("/equipos/3/mantenimientos").header(HttpHeaders.AUTHORIZATION, token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].horometroAlRegistrar").value(500.0))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.last").value(true));
        verify(mantenimientoService).listarPorEquipo(3L, PageRequest.of(0, 20));
    }

    @Test
    @DisplayName("GET /equipos/{id}/mantenimientos?page=1&size=10 pide exactamente esa página")
    void historialPasaPageYSize() throws Exception {
        // Arrange
        when(mantenimientoService.listarPorEquipo(eq(3L), any())).thenReturn(new PaginaMantenimientoResponse());

        // Act
        mockMvc.perform(get("/equipos/3/mantenimientos?page=1&size=10").header(HttpHeaders.AUTHORIZATION, token()))
                .andExpect(status().isOk());

        // Assert
        verify(mantenimientoService).listarPorEquipo(3L, PageRequest.of(1, 10));
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"page=-1", "size=0", "size=101"})
    @DisplayName("GET /equipos/{id}/mantenimientos con paginación fuera de rango: 400")
    void historialPaginacionInvalida(String consulta) throws Exception {
        // Act + Assert
        mockMvc.perform(get("/equipos/3/mantenimientos?" + consulta).header(HttpHeaders.AUTHORIZATION, token()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDACION_FALLIDA"));
    }

    @Test
    @DisplayName("GET /equipos/{id}/mantenimientos: 404 si el equipo no existe y 400 si el id es 0")
    void historialConErrores() throws Exception {
        // Arrange
        when(mantenimientoService.listarPorEquipo(eq(9L), any())).thenThrow(new EquipoNotFoundException(9L));

        // Act + Assert
        mockMvc.perform(get("/equipos/9/mantenimientos").header(HttpHeaders.AUTHORIZATION, token()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/equipos/0/mantenimientos").header(HttpHeaders.AUTHORIZATION, token()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("sin token: 401")
    void sinToken() throws Exception {
        // Act + Assert
        mockMvc.perform(get("/equipos/3/mantenimientos")).andExpect(status().isUnauthorized());
    }
}
