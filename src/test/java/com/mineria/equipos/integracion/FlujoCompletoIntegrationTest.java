package com.mineria.equipos.integracion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;
import com.mineria.equipos.auth.entity.Rol;
import com.mineria.equipos.testsupport.Tokens;

/**
 * Recorre la API completa con el contexto real de Spring, Flyway y H2 en modo MySQL:
 * login con el admin creado desde el entorno, CRUD, regla de mantenimiento y errores.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FlujoCompletoIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private String login() throws Exception {
        String respuesta = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin-test\",\"password\":\"clave-de-prueba-1\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(respuesta, "$.accessToken");
    }

    private String crearEquipo(String bearer, String codigo, int tipoId, String horometro) throws Exception {
        String respuesta = mockMvc.perform(post("/equipos").header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"" + codigo + "\",\"tipoId\":" + tipoId + ",\"horometro\":" + horometro + "}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return String.valueOf((Object) JsonPath.read(respuesta, "$.id"));
    }

    @Test
    @DisplayName("ciclo completo: alta, umbral de mantenimiento, reinicio del ciclo y baja con cascada")
    void cicloCompleto() throws Exception {
        // Arrange
        String bearer = login();

        // Act + Assert: un camión (intervalo 250 h) con 249.9 h todavía está OK
        String id = crearEquipo(bearer, "CAM-INT-1", 1, "249.9");
        mockMvc.perform(get("/equipos/" + id).header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requiereMantenimiento").value(false))
                .andExpect(jsonPath("$.estado").value("OK"))
                .andExpect(jsonPath("$.tipo.nombre").value("Camión"));

        // al llegar a 250 h exactas ya requiere mantenimiento
        mockMvc.perform(put("/equipos/" + id).header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"CAM-INT-1\",\"horometro\":250.0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requiereMantenimiento").value(true))
                .andExpect(jsonPath("$.estado").value("Requiere mantenimiento"));

        // no se puede bajar el horómetro
        mockMvc.perform(put("/equipos/" + id).header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"CAM-INT-1\",\"horometro\":100.0}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("HOROMETRO_NO_PUEDE_BAJAR"));

        // registrar el mantenimiento guarda el horómetro actual y reinicia el ciclo
        mockMvc.perform(post("/equipos/" + id + "/mantenimientos").header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"notas\":\"Cambio de aceite\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.horometroAlRegistrar").value(250.0))
                .andExpect(jsonPath("$.notas").value("Cambio de aceite"));
        mockMvc.perform(get("/equipos/" + id).header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(jsonPath("$.horometroUltimoMant").value(250.0))
                .andExpect(jsonPath("$.requiereMantenimiento").value(false))
                .andExpect(jsonPath("$.estado").value("OK"));
        mockMvc.perform(get("/equipos/" + id + "/mantenimientos").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));

        // el nuevo ciclo vuelve a vencer 250 h después del mantenimiento
        mockMvc.perform(put("/equipos/" + id).header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"CAM-INT-1\",\"horometro\":500.0}"))
                .andExpect(jsonPath("$.requiereMantenimiento").value(true));

        // eliminar el equipo borra también su historial
        mockMvc.perform(delete("/equipos/" + id).header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/equipos/" + id).header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/equipos/" + id + "/mantenimientos").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("el catálogo trae los 3 tipos iniciales y la lista de equipos incluye el estado")
    void catalogoYListado() throws Exception {
        // Arrange
        String bearer = login();
        crearEquipo(bearer, "EXC-INT-1", 2, "10.0");

        // Act + Assert
        mockMvc.perform(get("/tipos").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.nombre=='Excavadora')].intervaloHoras").value(500));
        // Las pruebas de integración comparten la base y el orden en que corren cambia según el sistema, así que
        // pido la página más grande para que el equipo recién creado no quede fuera de la primera de 20
        mockMvc.perform(get("/equipos").param("size", "100").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.codigo=='EXC-INT-1')].estado").value("OK"));
    }

    @Test
    @DisplayName("errores: código duplicado 409, tipo inexistente 404, sin token 401, login malo 401")
    void errores() throws Exception {
        // Arrange
        String bearer = login();
        crearEquipo(bearer, "PER-INT-1", 3, "5.0");

        // Act + Assert
        mockMvc.perform(post("/equipos").header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"PER-INT-1\",\"tipoId\":3,\"horometro\":1}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CODIGO_DUPLICADO"));
        mockMvc.perform(post("/equipos").header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"PER-INT-2\",\"tipoId\":99,\"horometro\":1}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TIPO_EQUIPO_NO_ENCONTRADO"));
        mockMvc.perform(get("/equipos")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("NO_AUTENTICADO"));
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin-test\",\"password\":\"incorrecta\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("CREDENCIALES_INVALIDAS"));
    }

    @Test
    @DisplayName("un OPERADOR puede consultar pero no eliminar (403)")
    void operadorNoEliminaEquipos() throws Exception {
        // Arrange
        String admin = login();
        String id = crearEquipo(admin, "CAM-INT-9", 1, "1.0");
        String operador = Tokens.bearer(Tokens.valido("operador", Rol.OPERADOR));

        // Act + Assert
        mockMvc.perform(get("/equipos/" + id).header(HttpHeaders.AUTHORIZATION, operador)).andExpect(status().isOk());
        mockMvc.perform(delete("/equipos/" + id).header(HttpHeaders.AUTHORIZATION, operador))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESO_DENEGADO"));
    }

    @Test
    @DisplayName("/health responde UP con la base de datos conectada, sin token")
    void health() throws Exception {
        // Act + Assert
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.version").value("test"));
    }

    @Test
    @DisplayName("el hash BCrypt de la contraseña nunca se expone en la API")
    void noExponeElHash() throws Exception {
        // Act
        String respuesta = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin-test\",\"password\":\"clave-de-prueba-1\"}"))
                .andReturn().getResponse().getContentAsString();

        // Assert
        assertThat(respuesta).doesNotContain("$2a$").doesNotContain("password");
    }
}
