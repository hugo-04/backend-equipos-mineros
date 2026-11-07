package com.mineria.equipos.infrastructure.adapter.in.web;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.mineria.equipos.application.port.in.ActualizarEquipoUseCase;
import com.mineria.equipos.application.port.in.ConsultarEquiposUseCase;
import com.mineria.equipos.application.port.in.CrearEquipoUseCase;
import com.mineria.equipos.application.port.in.EliminarEquipoUseCase;
import com.mineria.equipos.application.view.EquipoView;
import com.mineria.equipos.shared.exception.CodigoDuplicadoException;
import com.mineria.equipos.shared.exception.EquipoNotFoundException;
import com.mineria.equipos.shared.exception.HorometroInvalidoException;
import com.mineria.equipos.shared.exception.TipoEquipoNotFoundException;
import com.mineria.equipos.domain.model.Rol;
import com.mineria.equipos.testsupport.ControllerTest;
import com.mineria.equipos.testsupport.Datos;
import com.mineria.equipos.testsupport.Tokens;

@ControllerTest(EquiposController.class)
class EquiposControllerTest {

    private static final String BODY_VALIDO = """
            {"codigo":"CAM-001","tipoId":1,"horometro":120.5}""";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CrearEquipoUseCase crearEquipo;
    @MockitoBean
    private ActualizarEquipoUseCase actualizarEquipo;
    @MockitoBean
    private EliminarEquipoUseCase eliminarEquipo;
    @MockitoBean
    private ConsultarEquiposUseCase consultarEquipos;

    private String admin() {
        return Tokens.bearer(Tokens.valido("admin", Rol.ADMIN));
    }

    private String operador() {
        return Tokens.bearer(Tokens.valido("operador", Rol.OPERADOR));
    }

    private EquipoView vista(Long id, boolean requiere) {
        return Datos.equipoView(id, "CAM-00" + id, "300.0", "0", 250).toBuilder()
                .requiereMantenimiento(requiere)
                .estado(requiere ? "Requiere mantenimiento" : "OK")
                .build();
    }

    // ---------- GET ----------

    @Test
    @DisplayName("GET /equipos: 200 con la lista y el estado de mantenimiento")
    void listar() throws Exception {
        // Arrange
        when(consultarEquipos.listar()).thenReturn(List.of(vista(1L, true), vista(2L, false)));

        // Act + Assert
        mockMvc.perform(get("/equipos").header(HttpHeaders.AUTHORIZATION, admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].codigo").value("CAM-001"))
                .andExpect(jsonPath("$[0].tipo.nombre").value("Camión"))
                .andExpect(jsonPath("$[0].requiereMantenimiento").value(true))
                .andExpect(jsonPath("$[0].estado").value("Requiere mantenimiento"))
                .andExpect(jsonPath("$[1].estado").value("OK"));
    }

    @Test
    @DisplayName("GET /equipos/{id}: 200 con el equipo")
    void obtener() throws Exception {
        // Arrange
        when(consultarEquipos.obtener(7L)).thenReturn(vista(7L, false));

        // Act + Assert
        mockMvc.perform(get("/equipos/7").header(HttpHeaders.AUTHORIZATION, operador()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.requiereMantenimiento").value(false));
    }

    @Test
    @DisplayName("GET /equipos/{id}: 404 con el formato de error común si no existe")
    void obtenerInexistente() throws Exception {
        // Arrange
        when(consultarEquipos.obtener(99L)).thenThrow(new EquipoNotFoundException(99L));

        // Act + Assert
        mockMvc.perform(get("/equipos/99").header(HttpHeaders.AUTHORIZATION, admin()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.code").value("EQUIPO_NO_ENCONTRADO"))
                .andExpect(jsonPath("$.message").value("No existe un equipo con id 99"))
                .andExpect(jsonPath("$.path").value("/equipos/99"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.details").isEmpty());
    }

    @ParameterizedTest(name = "id={0}")
    @CsvSource({"0", "-5"})
    @DisplayName("GET /equipos/{id}: 400 si el id no es mayor que 0")
    void idNoPositivo(String id) throws Exception {
        // Act + Assert
        mockMvc.perform(get("/equipos/" + id).header(HttpHeaders.AUTHORIZATION, admin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDACION_FALLIDA"))
                .andExpect(jsonPath("$.details[0].field").value("id"));
        verify(consultarEquipos, never()).obtener(any());
    }

    @Test
    @DisplayName("GET /equipos/{id}: 400 si el id no es numérico")
    void idNoNumerico() throws Exception {
        // Act + Assert
        mockMvc.perform(get("/equipos/abc").header(HttpHeaders.AUTHORIZATION, admin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PARAMETRO_INVALIDO"))
                .andExpect(jsonPath("$.details[0].field").value("id"));
    }

    @Test
    @DisplayName("sin token a un endpoint protegido: 401 con el formato común")
    void sinToken() throws Exception {
        // Act + Assert
        mockMvc.perform(get("/equipos"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("NO_AUTENTICADO"))
                .andExpect(jsonPath("$.path").value("/equipos"));
    }

    // ---------- POST ----------

    @Test
    @DisplayName("POST /equipos: 201, Location y el comando correcto al caso de uso")
    void crear() throws Exception {
        // Arrange
        when(crearEquipo.crear(any())).thenReturn(vista(1L, true));

        // Act + Assert
        mockMvc.perform(post("/equipos").header(HttpHeaders.AUTHORIZATION, admin())
                        .contentType(MediaType.APPLICATION_JSON).content(BODY_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, endsWith("/equipos/1")))
                .andExpect(jsonPath("$.codigo").value("CAM-001"));
        ArgumentCaptor<CrearEquipoUseCase.Comando> comando = ArgumentCaptor.forClass(CrearEquipoUseCase.Comando.class);
        verify(crearEquipo).crear(comando.capture());
        assertThat(comando.getValue()).isEqualTo(new CrearEquipoUseCase.Comando("CAM-001", 1L, new BigDecimal("120.5")));
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', textBlock = """
            codigo con espacio        | {"codigo":"CAM 001","tipoId":1,"horometro":10}        | codigo
            codigo con simbolos       | {"codigo":"CAM_001!","tipoId":1,"horometro":10}       | codigo
            codigo muy corto          | {"codigo":"AB","tipoId":1,"horometro":10}             | codigo
            codigo muy largo          | {"codigo":"ABCDEFGHIJKLMNOPQRSTUVWXYZ-12345","tipoId":1,"horometro":10} | codigo
            codigo vacio              | {"codigo":"","tipoId":1,"horometro":10}               | codigo
            codigo ausente            | {"tipoId":1,"horometro":10}                           | codigo
            codigo nulo               | {"codigo":null,"tipoId":1,"horometro":10}             | codigo
            tipoId cero               | {"codigo":"CAM-001","tipoId":0,"horometro":10}        | tipoId
            tipoId negativo           | {"codigo":"CAM-001","tipoId":-3,"horometro":10}       | tipoId
            tipoId ausente            | {"codigo":"CAM-001","horometro":10}                   | tipoId
            horometro negativo        | {"codigo":"CAM-001","tipoId":1,"horometro":-0.1}      | horometro
            horometro nulo            | {"codigo":"CAM-001","tipoId":1,"horometro":null}      | horometro
            horometro ausente         | {"codigo":"CAM-001","tipoId":1}                       | horometro
            horometro con 2 decimales | {"codigo":"CAM-001","tipoId":1,"horometro":10.25}     | horometro
            horometro sobre el maximo | {"codigo":"CAM-001","tipoId":1,"horometro":1000000}   | horometro
            """)
    @DisplayName("POST /equipos: 400 con el detalle por campo cuando el body no cumple el contrato")
    void bodyInvalido(String caso, String body, String campo) throws Exception {
        // Act + Assert
        mockMvc.perform(post("/equipos").header(HttpHeaders.AUTHORIZATION, admin())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDACION_FALLIDA"))
                .andExpect(jsonPath("$.details[?(@.field=='" + campo + "')]").isNotEmpty());
        verify(crearEquipo, never()).crear(any());
    }

    @Test
    @DisplayName("POST /equipos: acepta los extremos válidos (3 y 30 caracteres, 0 y 999999.9)")
    void extremosValidos() throws Exception {
        // Arrange
        when(crearEquipo.crear(any())).thenReturn(vista(1L, false));
        String treintaCaracteres = "A".repeat(30);

        // Act + Assert
        mockMvc.perform(post("/equipos").header(HttpHeaders.AUTHORIZATION, admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"A-1\",\"tipoId\":1,\"horometro\":0}"))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/equipos").header(HttpHeaders.AUTHORIZATION, admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"" + treintaCaracteres + "\",\"tipoId\":1,\"horometro\":999999.9}"))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("POST /equipos: 400 si el body trae propiedades desconocidas")
    void propiedadDesconocida() throws Exception {
        // Act + Assert
        mockMvc.perform(post("/equipos").header(HttpHeaders.AUTHORIZATION, admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"CAM-001\",\"tipoId\":1,\"horometro\":10,\"rol\":\"admin\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PROPIEDAD_DESCONOCIDA"))
                .andExpect(jsonPath("$.details[0].field").value("rol"));
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', textBlock = """
            horometro como texto     | {"codigo":"CAM-001","tipoId":1,"horometro":"abc"}
            horometro numerico texto | {"codigo":"CAM-001","tipoId":1,"horometro":"12"}
            tipoId como texto        | {"codigo":"CAM-001","tipoId":"uno","horometro":10}
            """)
    @DisplayName("POST /equipos: 400 si un campo llega con el tipo equivocado")
    void tipoIncorrecto(String caso, String body) throws Exception {
        // Act + Assert
        mockMvc.perform(post("/equipos").header(HttpHeaders.AUTHORIZATION, admin())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("JSON_INVALIDO"))
                .andExpect(jsonPath("$.details[0].field").exists());
    }

    @Test
    @DisplayName("POST /equipos: 400 si el JSON está malformado")
    void jsonMalformado() throws Exception {
        // Act + Assert
        mockMvc.perform(post("/equipos").header(HttpHeaders.AUTHORIZATION, admin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"codigo\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("JSON_INVALIDO"))
                .andExpect(jsonPath("$.message").value("El cuerpo de la petición no es un JSON válido"));
    }

    @Test
    @DisplayName("POST /equipos: 400 si no hay body")
    void sinBody() throws Exception {
        // Act + Assert
        mockMvc.perform(post("/equipos").header(HttpHeaders.AUTHORIZATION, admin())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("JSON_INVALIDO"));
    }

    @Test
    @DisplayName("POST /equipos: 409 si el código está duplicado")
    void codigoDuplicado() throws Exception {
        // Arrange
        when(crearEquipo.crear(any())).thenThrow(new CodigoDuplicadoException("CAM-001"));

        // Act + Assert
        mockMvc.perform(post("/equipos").header(HttpHeaders.AUTHORIZATION, admin())
                        .contentType(MediaType.APPLICATION_JSON).content(BODY_VALIDO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CODIGO_DUPLICADO"));
    }

    @Test
    @DisplayName("POST /equipos: 404 si el tipo no existe")
    void tipoInexistente() throws Exception {
        // Arrange
        when(crearEquipo.crear(any())).thenThrow(new TipoEquipoNotFoundException(1L));

        // Act + Assert
        mockMvc.perform(post("/equipos").header(HttpHeaders.AUTHORIZATION, admin())
                        .contentType(MediaType.APPLICATION_JSON).content(BODY_VALIDO))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TIPO_EQUIPO_NO_ENCONTRADO"));
    }

    // ---------- PUT ----------

    @Test
    @DisplayName("PUT /equipos/{id}: 200 y el comando lleva el id del path")
    void actualizar() throws Exception {
        // Arrange
        when(actualizarEquipo.actualizar(any())).thenReturn(vista(4L, false));

        // Act + Assert
        mockMvc.perform(put("/equipos/4").header(HttpHeaders.AUTHORIZATION, operador())
                        .contentType(MediaType.APPLICATION_JSON).content(BODY_VALIDO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(4));
        ArgumentCaptor<ActualizarEquipoUseCase.Comando> comando =
                ArgumentCaptor.forClass(ActualizarEquipoUseCase.Comando.class);
        verify(actualizarEquipo).actualizar(comando.capture());
        assertThat(comando.getValue().id()).isEqualTo(4L);
    }

    @Test
    @DisplayName("PUT /equipos/{id}: 422 si el horómetro baja")
    void actualizarBajandoHorometro() throws Exception {
        // Arrange
        when(actualizarEquipo.actualizar(any())).thenThrow(new HorometroInvalidoException("El horómetro no puede bajar"));

        // Act + Assert
        mockMvc.perform(put("/equipos/4").header(HttpHeaders.AUTHORIZATION, admin())
                        .contentType(MediaType.APPLICATION_JSON).content(BODY_VALIDO))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.code").value("HOROMETRO_INVALIDO"));
    }

    @Test
    @DisplayName("PUT /equipos/{id}: 404 si el equipo no existe")
    void actualizarInexistente() throws Exception {
        // Arrange
        when(actualizarEquipo.actualizar(any())).thenThrow(new EquipoNotFoundException(4L));

        // Act + Assert
        mockMvc.perform(put("/equipos/4").header(HttpHeaders.AUTHORIZATION, admin())
                        .contentType(MediaType.APPLICATION_JSON).content(BODY_VALIDO))
                .andExpect(status().isNotFound());
    }

    // ---------- DELETE ----------

    @Test
    @DisplayName("DELETE /equipos/{id}: 204 para un ADMIN")
    void eliminar() throws Exception {
        // Act + Assert
        mockMvc.perform(delete("/equipos/4").header(HttpHeaders.AUTHORIZATION, admin()))
                .andExpect(status().isNoContent());
        verify(eliminarEquipo).eliminar(4L);
    }

    @Test
    @DisplayName("DELETE /equipos/{id}: 403 con el formato común para un OPERADOR")
    void eliminarSinPermiso() throws Exception {
        // Act + Assert
        mockMvc.perform(delete("/equipos/4").header(HttpHeaders.AUTHORIZATION, operador()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("ACCESO_DENEGADO"))
                .andExpect(jsonPath("$.path").value("/equipos/4"));
        verify(eliminarEquipo, never()).eliminar(any());
    }

    @Test
    @DisplayName("DELETE /equipos/{id}: 404 si no existe")
    void eliminarInexistente() throws Exception {
        // Arrange
        org.mockito.Mockito.doThrow(new EquipoNotFoundException(4L)).when(eliminarEquipo).eliminar(4L);

        // Act + Assert
        mockMvc.perform(delete("/equipos/4").header(HttpHeaders.AUTHORIZATION, admin()))
                .andExpect(status().isNotFound());
    }
}
