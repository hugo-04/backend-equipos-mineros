package com.mineria.equipos.integracion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

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

/** Los tres listados paginados de punta a punta: contexto real, Flyway, H2 en modo MySQL y la cadena de seguridad. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaginacionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private String login() throws Exception {
        String respuesta = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin-test\",\"password\":\"clave-de-prueba-1\"}"))
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(respuesta, "$.accessToken");
    }

    private String json(String bearer, String ruta) throws Exception {
        return mockMvc.perform(get(ruta).header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private long crearEquipo(String bearer, String codigo) throws Exception {
        String respuesta = mockMvc.perform(post("/equipos").header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"" + codigo + "\",\"tipoId\":1,\"horometro\":10}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(respuesta, "$.id")).longValue();
    }

    @Test
    @DisplayName("GET /tipos?size=2: 3 tipos iniciales en 2 páginas")
    void tipos() throws Exception {
        // Arrange
        String bearer = login();

        // Act
        String primera = json(bearer, "/tipos?page=0&size=2");
        String segunda = json(bearer, "/tipos?page=1&size=2");

        // Assert
        assertThat((List<?>) JsonPath.read(primera, "$.content")).hasSize(2);
        assertThat((Integer) JsonPath.read(primera, "$.totalPages")).isEqualTo(2);
        assertThat((Integer) JsonPath.read(primera, "$.totalElements")).isEqualTo(3);
        assertThat((Boolean) JsonPath.read(primera, "$.first")).isTrue();
        assertThat((Boolean) JsonPath.read(primera, "$.last")).isFalse();
        assertThat((List<?>) JsonPath.read(segunda, "$.content")).hasSize(1);
        assertThat((Integer) JsonPath.read(segunda, "$.page")).isEqualTo(1);
        assertThat((Boolean) JsonPath.read(segunda, "$.last")).isTrue();
    }

    @Test
    @DisplayName("GET /equipos: recorriendo todas las páginas aparece cada equipo una sola vez, en orden de id")
    void equiposSinRepetirNiPerder() throws Exception {
        // Arrange
        String bearer = login();
        for (int i = 1; i <= 5; i++) {
            crearEquipo(bearer, "RECORR-" + i);
        }

        // Act: se recorren todas las páginas de a 2
        List<String> codigos = new ArrayList<>();
        List<Number> ids = new ArrayList<>();
        int pagina = 0;
        boolean ultima;
        do {
            String cuerpo = json(bearer, "/equipos?size=2&page=" + pagina++);
            codigos.addAll(JsonPath.read(cuerpo, "$.content[*].codigo"));
            ids.addAll(JsonPath.read(cuerpo, "$.content[*].id"));
            ultima = JsonPath.read(cuerpo, "$.last");
        } while (!ultima && pagina < 50);

        // Assert
        assertThat(codigos).filteredOn(c -> c.startsWith("RECORR-")).containsExactlyInAnyOrder(
                "RECORR-1", "RECORR-2", "RECORR-3", "RECORR-4", "RECORR-5");
        assertThat(codigos).doesNotHaveDuplicates();
        assertThat(ids).isSortedAccordingTo((a, b) -> Long.compare(a.longValue(), b.longValue()));
    }

    @Test
    @DisplayName("GET /equipos sin parámetros devuelve página 0 de tamaño 20 y una página fuera de rango viene vacía")
    void valoresPorDefectoYFueraDeRango() throws Exception {
        // Arrange
        String bearer = login();
        crearEquipo(bearer, "PAGIN-DEF-1");

        // Act
        String porDefecto = json(bearer, "/equipos");
        String lejana = json(bearer, "/equipos?page=9999");

        // Assert
        assertThat((Integer) JsonPath.read(porDefecto, "$.page")).isZero();
        assertThat((Integer) JsonPath.read(porDefecto, "$.size")).isEqualTo(20);
        assertThat((List<?>) JsonPath.read(lejana, "$.content")).isEmpty();
        assertThat(((Number) JsonPath.read(lejana, "$.totalElements")).longValue()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("el historial de mantenimientos también se pagina (más reciente primero)")
    void historial() throws Exception {
        // Arrange
        String bearer = login();
        long id = crearEquipo(bearer, "PAGIN-MANT-1");
        for (int i = 1; i <= 3; i++) {
            mockMvc.perform(post("/equipos/" + id + "/mantenimientos").header(HttpHeaders.AUTHORIZATION, bearer)
                            .contentType(MediaType.APPLICATION_JSON).content("{\"notas\":\"mant " + i + "\"}"))
                    .andExpect(status().isCreated());
        }

        // Act
        String primera = json(bearer, "/equipos/" + id + "/mantenimientos?size=2&page=0");
        String segunda = json(bearer, "/equipos/" + id + "/mantenimientos?size=2&page=1");

        // Assert: el último registrado sale primero
        assertThat((List<String>) JsonPath.read(primera, "$.content[*].notas")).containsExactly("mant 3", "mant 2");
        assertThat((List<String>) JsonPath.read(segunda, "$.content[*].notas")).containsExactly("mant 1");
        assertThat((Integer) JsonPath.read(primera, "$.totalElements")).isEqualTo(3);
        assertThat((Boolean) JsonPath.read(segunda, "$.last")).isTrue();
    }

    @Test
    @DisplayName("size mayor a 100 o page negativa: 400 con el detalle del parámetro")
    void limitesDelContrato() throws Exception {
        // Arrange
        String bearer = login();

        // Act + Assert
        mockMvc.perform(get("/equipos?size=101").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDACION_FALLIDA"))
                .andExpect(jsonPath("$.details[0].field").value("size"));
        mockMvc.perform(get("/tipos?page=-1").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("page"));
        mockMvc.perform(get("/equipos?size=100").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk());
    }
}
