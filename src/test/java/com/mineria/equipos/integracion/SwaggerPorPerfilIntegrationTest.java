package com.mineria.equipos.integracion;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** La documentación Swagger/OpenAPI está disponible en desarrollo pero apagada por defecto en producción. */
class SwaggerPorPerfilIntegrationTest {

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    @ActiveProfiles("test")
    @DisplayName("perfil de desarrollo")
    class EnDesarrollo {

        @Autowired
        private MockMvc mockMvc;

        @Test
        @DisplayName("Swagger y api-docs responden sin token")
        void disponible() throws Exception {
            mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
            mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
        }

        @Test
        @DisplayName("la CSP estricta de la API no se aplica a Swagger UI (necesita cargar sus scripts), pero el resto de cabeceras sí")
        void cspNoRompeSwagger() throws Exception {
            mockMvc.perform(get("/swagger-ui/index.html"))
                    .andExpect(header().doesNotExist("Content-Security-Policy"))
                    .andExpect(header().string("X-Content-Type-Options", "nosniff"));
            mockMvc.perform(get("/health")).andExpect(header().exists("Content-Security-Policy"));
        }
    }

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    @ActiveProfiles({"test", "prod"})
    @DisplayName("perfil prod (SWAGGER_ENABLED sin definir)")
    class EnProduccion {

        @Autowired
        private MockMvc mockMvc;

        @Test
        @DisplayName("Swagger y api-docs NO existen (404)")
        void apagado() throws Exception {
            mockMvc.perform(get("/v3/api-docs")).andExpect(status().isNotFound());
            mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isNotFound());
            mockMvc.perform(get("/swagger-ui.html")).andExpect(status().isNotFound());
        }
    }

    @Nested
    @SpringBootTest(properties = "SWAGGER_ENABLED=true")
    @AutoConfigureMockMvc
    @ActiveProfiles({"test", "prod"})
    @DisplayName("perfil prod con SWAGGER_ENABLED=true")
    class EnProduccionEncendido {

        @Autowired
        private MockMvc mockMvc;

        @Test
        @DisplayName("se puede encender de forma explícita")
        void encendidoExplicito() throws Exception {
            mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
        }
    }
}
