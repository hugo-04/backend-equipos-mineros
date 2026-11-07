package com.mineria.equipos.auth.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.mineria.equipos.auth.controller.AuthController;
import com.mineria.equipos.auth.service.AuthService;
import com.mineria.equipos.generated.model.LoginResponse;
import com.mineria.equipos.shared.exception.CredencialesInvalidasException;
import com.mineria.equipos.testsupport.ControllerTest;

/** Límite de intentos de login sobre la cadena real de filtros: 3 fallos por IP y luego 429. */
@ControllerTest(AuthController.class)
@TestPropertySource(properties = {"app.security.login-max-intentos=3", "app.security.login-ventana-segundos=300"})
class LimiteIntentosLoginHttpTest {

    private static final String LOGIN_MALO = "{\"username\":\"admin\",\"password\":\"mala\"}";
    private static final String LOGIN_BUENO = "{\"username\":\"admin\",\"password\":\"buena\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    private RequestPostProcessor desde(String ip) {
        return request -> {
            request.setRemoteAddr(ip);
            return request;
        };
    }

    private void loginFallido(String ip) throws Exception {
        doThrow(new CredencialesInvalidasException()).when(authService).login(any());
        mockMvc.perform(post("/auth/login").with(desde(ip)).contentType(MediaType.APPLICATION_JSON).content(LOGIN_MALO))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("tras 3 logins fallidos la IP recibe 429 con Retry-After y el servicio ya no se consulta")
    void bloqueaTrasElMaximo() throws Exception {
        // Arrange + Act: 3 fallos permitidos
        loginFallido("10.0.0.1");
        loginFallido("10.0.0.1");
        loginFallido("10.0.0.1");

        // Assert: el 4.º intento ya ni llega a revisar la contraseña
        mockMvc.perform(post("/auth/login").with(desde("10.0.0.1")).contentType(MediaType.APPLICATION_JSON).content(LOGIN_MALO))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.code").value("DEMASIADOS_INTENTOS"))
                .andExpect(jsonPath("$.path").value("/auth/login"));
        verify(authService, times(3)).login(any());
    }

    @Test
    @DisplayName("el bloqueo también frena un intento con la contraseña correcta")
    void bloqueaAunConClaveCorrecta() throws Exception {
        // Arrange
        loginFallido("10.0.0.2");
        loginFallido("10.0.0.2");
        loginFallido("10.0.0.2");
        doReturn(new LoginResponse("jwt", "Bearer", 1800L)).when(authService).login(any());

        // Act + Assert
        mockMvc.perform(post("/auth/login").with(desde("10.0.0.2")).contentType(MediaType.APPLICATION_JSON).content(LOGIN_BUENO))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("otra IP no se ve afectada por el bloqueo")
    void otraIpNoSeBloquea() throws Exception {
        // Arrange
        loginFallido("10.0.0.3");
        loginFallido("10.0.0.3");
        loginFallido("10.0.0.3");
        doReturn(new LoginResponse("jwt", "Bearer", 1800L)).when(authService).login(any());

        // Act + Assert
        mockMvc.perform(post("/auth/login").with(desde("10.0.0.99")).contentType(MediaType.APPLICATION_JSON).content(LOGIN_BUENO))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("un login correcto reinicia el conteo de fallos")
    void exitoReiniciaElConteo() throws Exception {
        // Arrange: 2 fallos y un acierto
        loginFallido("10.0.0.4");
        loginFallido("10.0.0.4");
        doReturn(new LoginResponse("jwt", "Bearer", 1800L)).when(authService).login(any());
        mockMvc.perform(post("/auth/login").with(desde("10.0.0.4")).contentType(MediaType.APPLICATION_JSON).content(LOGIN_BUENO))
                .andExpect(status().isOk());

        // Act: otros 2 fallos NO deben bloquear (el conteo empezó de cero)
        loginFallido("10.0.0.4");
        loginFallido("10.0.0.4");

        // Assert
        doReturn(new LoginResponse("jwt", "Bearer", 1800L)).when(authService).login(any());
        mockMvc.perform(post("/auth/login").with(desde("10.0.0.4")).contentType(MediaType.APPLICATION_JSON).content(LOGIN_BUENO))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("un body inválido (400) no cuenta como intento fallido")
    void validacionNoCuenta() throws Exception {
        // Act: 5 bodies vacíos no deben bloquear
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/auth/login").with(desde("10.0.0.5")).contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isBadRequest());
        }

        // Assert
        doReturn(new LoginResponse("jwt", "Bearer", 1800L)).when(authService).login(any());
        mockMvc.perform(post("/auth/login").with(desde("10.0.0.5")).contentType(MediaType.APPLICATION_JSON).content(LOGIN_BUENO))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("el límite aplica solo a POST /auth/login: otros endpoints no se cuentan")
    void otrosEndpointsNoSeCuentan() throws Exception {
        // Act: muchos 401 en otra ruta no bloquean el login
        for (int i = 0; i < 6; i++) {
            mockMvc.perform(get("/equipos").with(desde("10.0.0.6"))).andExpect(status().isUnauthorized());
        }

        // Assert
        doReturn(new LoginResponse("jwt", "Bearer", 1800L)).when(authService).login(any());
        mockMvc.perform(post("/auth/login").with(desde("10.0.0.6")).contentType(MediaType.APPLICATION_JSON).content(LOGIN_BUENO))
                .andExpect(status().isOk());
    }
}
