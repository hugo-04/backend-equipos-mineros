package com.mineria.equipos.auth.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.mineria.equipos.auth.controller.AuthController;
import com.mineria.equipos.auth.entity.Rol;
import com.mineria.equipos.auth.service.AuthService;
import com.mineria.equipos.equipo.controller.EquipoController;
import com.mineria.equipos.equipo.service.EquipoService;
import com.mineria.equipos.generated.model.LoginResponse;
import com.mineria.equipos.testsupport.ControllerTest;
import com.mineria.equipos.testsupport.Tokens;

/** Un cuerpo más grande que el máximo (aquí 200 bytes) se rechaza con 413 antes de llegar al controller. */
@ControllerTest({AuthController.class, EquipoController.class})
@TestPropertySource(properties = "app.security.cuerpo-max-bytes=200")
class LimiteCuerpoHttpTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;
    @MockitoBean
    private EquipoService equipoService;

    /** Un login válido cuyo cuerpo mide exactamente {@code bytes} (el relleno va en la contraseña, que admite hasta 200). */
    private String loginConLongitud(int bytes) {
        String base = "{\"username\":\"admin\",\"password\":\"\"}";
        return "{\"username\":\"admin\",\"password\":\"" + "x".repeat(bytes - base.length()) + "\"}";
    }

    @Test
    @DisplayName("un cuerpo de 201 bytes recibe 413 con el formato de error común")
    void cuerpoMuyGrande() throws Exception {
        // Act + Assert
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginConLongitud(201)))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.status").value(413))
                .andExpect(jsonPath("$.code").value("CUERPO_DEMASIADO_GRANDE"))
                .andExpect(jsonPath("$.path").value("/auth/login"));
    }

    @Test
    @DisplayName("un cuerpo de exactamente 200 bytes sí pasa")
    void cuerpoEnElLimite() throws Exception {
        // Arrange
        doReturn(new LoginResponse("jwt", "Bearer", 1800L)).when(authService).login(any());

        // Act + Assert
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginConLongitud(200)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("un cuerpo sin Content-Length (chunked) se rechaza porque no se sabe cuánto pesa")
    void cuerpoChunked() throws Exception {
        // Act + Assert
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).header("Transfer-Encoding", "chunked")
                        .with(request -> {
                            request.setContent(null);
                            return request;
                        }))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.code").value("CUERPO_DEMASIADO_GRANDE"));
    }

    @Test
    @DisplayName("también protege los endpoints autenticados que llevan cuerpo")
    void tambienEnEquipos() throws Exception {
        // Arrange
        String token = Tokens.bearer(Tokens.valido("admin", Rol.ADMIN));
        String grande = "{\"codigo\":\"" + "A".repeat(300) + "\",\"tipoId\":1,\"horometro\":1}";

        // Act + Assert
        mockMvc.perform(post("/equipos").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(grande))
                .andExpect(status().isPayloadTooLarge());
    }

    @Test
    @DisplayName("los métodos sin cuerpo (DELETE) no pasan por el límite")
    void deleteNoSeLimita() throws Exception {
        // Arrange
        String token = Tokens.bearer(Tokens.valido("admin", Rol.ADMIN));

        // Act + Assert
        mockMvc.perform(delete("/equipos/4").header(HttpHeaders.AUTHORIZATION, token)).andExpect(status().isNoContent());
    }
}
