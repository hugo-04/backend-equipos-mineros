package com.mineria.equipos.integracion;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

/**
 * Las reglas de negocio de equipos y mantenimientos recorridas por HTTP con el contexto real, Flyway y H2 en
 * modo MySQL. Datos de ejemplo: camión 250 h (tipo 1), excavadora 500 h (tipo 2), perforadora 300 h (tipo 3).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReglasDeNegocioIntegrationTest {

    private static final int CAMION = 1;
    private static final int EXCAVADORA = 2;

    @Autowired
    private MockMvc mockMvc;

    private String bearer;

    @BeforeEach
    void iniciarSesion() throws Exception {
        String respuesta = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin-test\",\"password\":\"clave-de-prueba-1\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        bearer = "Bearer " + JsonPath.read(respuesta, "$.accessToken");
    }

    // Ayudas

    private String crear(String codigo, int tipoId, String horometro) throws Exception {
        String respuesta = mockMvc.perform(post("/equipos").header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"" + codigo + "\",\"tipoId\":" + tipoId + ",\"horometro\":" + horometro + "}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return String.valueOf((Object) JsonPath.read(respuesta, "$.id"));
    }

    private ResultActions editar(String id, String cuerpo) throws Exception {
        return mockMvc.perform(put("/equipos/" + id).header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    private ResultActions editarHorometro(String id, String codigo, String horometro) throws Exception {
        return editar(id, "{\"codigo\":\"" + codigo + "\",\"horometro\":" + horometro + "}");
    }

    private ResultActions consultar(String id) throws Exception {
        return mockMvc.perform(get("/equipos/" + id).header(HttpHeaders.AUTHORIZATION, bearer));
    }

    private ResultActions registrarMantenimiento(String id, String cuerpo) throws Exception {
        return mockMvc.perform(post("/equipos/" + id + "/mantenimientos").header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    // Reglas 1, 2 y 5: estado calculado y campos calculados

    @Test
    @DisplayName("camión con 100 h: OK y faltan 150 h; al editar a 260 h requiere mantenimiento")
    void camionConCienHoras() throws Exception {
        // Arrange
        String id = crear("RN-CAM-1", CAMION, "100.0");

        // Act + Assert: recién creado
        consultar(id).andExpect(status().isOk())
                .andExpect(jsonPath("$.horometroUltimoMant").value(0.0))
                .andExpect(jsonPath("$.horasDesdeUltimoMant").value(100.0))
                .andExpect(jsonPath("$.horasRestantes").value(150.0))
                .andExpect(jsonPath("$.requiereMantenimiento").value(false))
                .andExpect(jsonPath("$.estado").value("OK"));

        // Act + Assert: editado a 260 h, ya pasó el intervalo y las horas restantes no bajan de 0
        editarHorometro(id, "RN-CAM-1", "260.0").andExpect(status().isOk())
                .andExpect(jsonPath("$.horasDesdeUltimoMant").value(260.0))
                .andExpect(jsonPath("$.horasRestantes").value(0.0))
                .andExpect(jsonPath("$.requiereMantenimiento").value(true))
                .andExpect(jsonPath("$.estado").value("Requiere mantenimiento"));
    }

    @Test
    @DisplayName("exactamente en el intervalo (250 h) ya requiere mantenimiento, y 249.9 h todavía no")
    void exactamenteEnElIntervalo() throws Exception {
        // Arrange
        String id = crear("RN-CAM-2", CAMION, "249.9");

        // Act + Assert
        consultar(id).andExpect(jsonPath("$.requiereMantenimiento").value(false))
                .andExpect(jsonPath("$.horasRestantes").value(0.1));
        editarHorometro(id, "RN-CAM-2", "250.0").andExpect(status().isOk())
                .andExpect(jsonPath("$.requiereMantenimiento").value(true))
                .andExpect(jsonPath("$.horasRestantes").value(0.0));
    }

    @Test
    @DisplayName("un camión creado con 300 h requiere mantenimiento de inmediato (el ciclo arranca en 0)")
    void creadoYaVencido() throws Exception {
        // Act + Assert
        String id = crear("RN-CAM-3", CAMION, "300.0");
        consultar(id).andExpect(jsonPath("$.horometroUltimoMant").value(0.0))
                .andExpect(jsonPath("$.requiereMantenimiento").value(true))
                .andExpect(jsonPath("$.estado").value("Requiere mantenimiento"));
    }

    @Test
    @DisplayName("la lista trae los mismos campos calculados que el detalle")
    void listaConCamposCalculados() throws Exception {
        // Arrange
        crear("RN-EXC-1", EXCAVADORA, "120.0");

        // Act + Assert: la excavadora tiene intervalo de 500 h, así que faltan 380
        mockMvc.perform(get("/equipos?size=100").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.codigo=='RN-EXC-1')].horasDesdeUltimoMant").value(120.0))
                .andExpect(jsonPath("$.content[?(@.codigo=='RN-EXC-1')].horasRestantes").value(380.0))
                .andExpect(jsonPath("$.content[?(@.codigo=='RN-EXC-1')].requiereMantenimiento").value(false));
    }

    // Reglas 3 y 4: el horómetro nunca baja y el mantenimiento reinicia el ciclo, no el horómetro

    @Test
    @DisplayName("registrar mantenimiento con 260 h: el horómetro sigue en 260, el ciclo vuelve a 0 y queda un registro")
    void mantenimientoReiniciaElCicloNoElHorometro() throws Exception {
        // Arrange
        String id = crear("RN-CAM-4", CAMION, "100.0");
        editarHorometro(id, "RN-CAM-4", "260.0").andExpect(jsonPath("$.requiereMantenimiento").value(true));

        // Act
        registrarMantenimiento(id, "{\"notas\":\"Cambio de aceite\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.horometroAlRegistrar").value(260.0));

        // Assert: el horómetro no se tocó, el ciclo se reinició y el estado volvió a OK
        consultar(id).andExpect(jsonPath("$.horometro").value(260.0))
                .andExpect(jsonPath("$.horometroUltimoMant").value(260.0))
                .andExpect(jsonPath("$.horasDesdeUltimoMant").value(0.0))
                .andExpect(jsonPath("$.horasRestantes").value(250.0))
                .andExpect(jsonPath("$.requiereMantenimiento").value(false))
                .andExpect(jsonPath("$.estado").value("OK"));
        mockMvc.perform(get("/equipos/" + id + "/mantenimientos").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].horometroAlRegistrar").value(260.0));

        // Assert: 250 h después del mantenimiento (510 - 260) vuelve a requerirlo
        editarHorometro(id, "RN-CAM-4", "510.0").andExpect(status().isOk())
                .andExpect(jsonPath("$.horasDesdeUltimoMant").value(250.0))
                .andExpect(jsonPath("$.requiereMantenimiento").value(true));
    }

    @Test
    @DisplayName("editar con un horómetro menor al actual da 422 HOROMETRO_NO_PUEDE_BAJAR y no cambia nada")
    void horometroNoBaja() throws Exception {
        // Arrange
        String id = crear("RN-CAM-5", CAMION, "200.0");

        // Act + Assert
        editarHorometro(id, "RN-CAM-5", "199.9")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("HOROMETRO_NO_PUEDE_BAJAR"));
        consultar(id).andExpect(jsonPath("$.horometro").value(200.0));
    }

    // Regla 6: el tipo es inmutable; el código es único

    @Test
    @DisplayName("editar enviando tipoId da 400 por el contrato y el tipo no cambia en la base")
    void tipoIdEnLaEdicionEsRechazado() throws Exception {
        // Arrange
        String id = crear("RN-CAM-6", CAMION, "10.0");

        // Act + Assert
        editar(id, "{\"codigo\":\"RN-CAM-6\",\"tipoId\":2,\"horometro\":20.0}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PROPIEDAD_DESCONOCIDA"));
        consultar(id).andExpect(jsonPath("$.tipo.id").value(CAMION))
                .andExpect(jsonPath("$.tipo.nombre").value("Camión"))
                .andExpect(jsonPath("$.horometro").value(10.0));
    }

    @Test
    @DisplayName("editar solo cambia código y horómetro; el tipo y el ciclo se quedan igual")
    void editarSoloCodigoYHorometro() throws Exception {
        // Arrange
        String id = crear("RN-CAM-7", CAMION, "50.0");

        // Act + Assert
        editarHorometro(id, "RN-CAM-7B", "80.0").andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("RN-CAM-7B"))
                .andExpect(jsonPath("$.horometro").value(80.0))
                .andExpect(jsonPath("$.tipo.id").value(CAMION))
                .andExpect(jsonPath("$.horometroUltimoMant").value(0.0));
    }

    @Test
    @DisplayName("editar con un código que ya es de otro equipo da 409")
    void codigoRepetidoAlEditar() throws Exception {
        // Arrange
        crear("RN-DUP-A", CAMION, "1.0");
        String otro = crear("RN-DUP-B", CAMION, "1.0");

        // Act + Assert
        editarHorometro(otro, "RN-DUP-A", "2.0")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CODIGO_DUPLICADO"));
        consultar(otro).andExpect(jsonPath("$.codigo").value("RN-DUP-B"));
    }

    // Regla 7: notas opcionales

    @Test
    @DisplayName("mantenimiento sin notas, con notas vacías o con solo espacios se guarda con notas nulas")
    void notasOpcionales() throws Exception {
        // Arrange
        String id = crear("RN-NOT-1", CAMION, "10.0");

        // Act + Assert
        registrarMantenimiento(id, "{}").andExpect(status().isCreated())
                .andExpect(jsonPath("$.notas").doesNotExist());
        registrarMantenimiento(id, "{\"notas\":\"\"}").andExpect(status().isCreated())
                .andExpect(jsonPath("$.notas").doesNotExist());
        registrarMantenimiento(id, "{\"notas\":\"   \"}").andExpect(status().isCreated())
                .andExpect(jsonPath("$.notas").doesNotExist());
        registrarMantenimiento(id, "{\"notas\":\"  con espacios al borde  \"}").andExpect(status().isCreated())
                .andExpect(jsonPath("$.notas").value("con espacios al borde"));
    }

    @Test
    @DisplayName("notas de 500 caracteres se aceptan y de 501 dan 400 sin registrar el mantenimiento")
    void notasMaximo500() throws Exception {
        // Arrange
        String id = crear("RN-NOT-2", CAMION, "10.0");

        // Act + Assert
        registrarMantenimiento(id, "{\"notas\":\"" + "a".repeat(500) + "\"}").andExpect(status().isCreated());
        registrarMantenimiento(id, "{\"notas\":\"" + "a".repeat(501) + "\"}").andExpect(status().isBadRequest());
        mockMvc.perform(get("/equipos/" + id + "/mantenimientos").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(jsonPath("$.totalElements").value(1));
    }
}
