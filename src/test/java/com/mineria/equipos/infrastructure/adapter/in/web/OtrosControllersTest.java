package com.mineria.equipos.infrastructure.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.mineria.equipos.application.port.in.AutenticarUsuarioUseCase;
import com.mineria.equipos.application.port.in.ConsultarMantenimientosUseCase;
import com.mineria.equipos.application.port.in.ConsultarTiposUseCase;
import com.mineria.equipos.application.port.in.RegistrarMantenimientoUseCase;
import com.mineria.equipos.application.port.in.VerificarSaludUseCase;
import com.mineria.equipos.application.view.EstadoSalud;
import com.mineria.equipos.application.view.SesionView;
import com.mineria.equipos.application.view.TipoEquipoView;
import com.mineria.equipos.shared.exception.CredencialesInvalidasException;
import com.mineria.equipos.shared.exception.EquipoNotFoundException;
import com.mineria.equipos.domain.model.Rol;
import com.mineria.equipos.testsupport.ControllerTest;
import com.mineria.equipos.testsupport.Datos;
import com.mineria.equipos.testsupport.Tokens;

/** Controllers de login, health, tipos y mantenimientos. */
@ControllerTest({AuthController.class, SaludController.class, TiposController.class, MantenimientosController.class})
class OtrosControllersTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AutenticarUsuarioUseCase autenticarUsuario;
    @MockitoBean
    private VerificarSaludUseCase verificarSalud;
    @MockitoBean
    private ConsultarTiposUseCase consultarTipos;
    @MockitoBean
    private RegistrarMantenimientoUseCase registrarMantenimiento;
    @MockitoBean
    private ConsultarMantenimientosUseCase consultarMantenimientos;

    private String token() {
        return Tokens.bearer(Tokens.valido("admin", Rol.ADMIN));
    }

    // ---------- /auth/login ----------

    @Test
    @DisplayName("POST /auth/login: 200 con el token, sin necesidad de autenticación")
    void loginCorrecto() throws Exception {
        // Arrange
        when(autenticarUsuario.autenticar(any()))
                .thenReturn(SesionView.builder().accessToken("jwt-de-prueba").tokenType("Bearer").expiresIn(3600).build());

        // Act + Assert
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"clave\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt-de-prueba"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600));
    }

    @Test
    @DisplayName("POST /auth/login: 401 con el formato común si las credenciales son incorrectas")
    void loginIncorrecto() throws Exception {
        // Arrange
        when(autenticarUsuario.autenticar(any())).thenThrow(new CredencialesInvalidasException());

        // Act + Assert
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"mala\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("CREDENCIALES_INVALIDAS"))
                .andExpect(jsonPath("$.path").value("/auth/login"));
    }

    @Test
    @DisplayName("POST /auth/login: 400 si username o password están vacíos, en blanco o ausentes")
    void loginInvalido() throws Exception {
        // Act + Assert
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"password\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[?(@.field=='username')]").isNotEmpty())
                .andExpect(jsonPath("$.details[?(@.field=='password')]").isNotEmpty());
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDACION_FALLIDA"));
    }

    // ---------- /health ----------

    @Test
    @DisplayName("GET /health: 200 UP y la versión, público")
    void healthUp() throws Exception {
        // Arrange
        when(verificarSalud.verificar()).thenReturn(EstadoSalud.UP);

        // Act + Assert
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.version").value("test"));
    }

    @Test
    @DisplayName("GET /health: 503 DOWN si la base de datos no responde")
    void healthDown() throws Exception {
        // Arrange
        when(verificarSalud.verificar()).thenReturn(EstadoSalud.DOWN);

        // Act + Assert
        mockMvc.perform(get("/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("DOWN"));
    }

    // ---------- /tipos ----------

    @Test
    @DisplayName("GET /tipos: 200 con el catálogo")
    void tipos() throws Exception {
        // Arrange
        when(consultarTipos.listar()).thenReturn(List.of(
                TipoEquipoView.builder().id(1L).nombre("Camión").intervaloHoras(250).build(),
                TipoEquipoView.builder().id(2L).nombre("Excavadora").intervaloHoras(500).build()));

        // Act + Assert
        mockMvc.perform(get("/tipos").header(HttpHeaders.AUTHORIZATION, token()))
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

    // ---------- /equipos/{id}/mantenimientos ----------

    @Test
    @DisplayName("POST /equipos/{id}/mantenimientos: 201 con el registro del historial")
    void registrarMantenimientoCreado() throws Exception {
        // Arrange
        when(registrarMantenimiento.registrar(any())).thenReturn(Datos.mantenimientoView(5L, 3L, "300.0"));

        // Act + Assert
        mockMvc.perform(post("/equipos/3/mantenimientos").header(HttpHeaders.AUTHORIZATION, token())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"notas\":\"Cambio de aceite\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.equipoId").value(3))
                .andExpect(jsonPath("$.horometroAlRegistrar").value(300.0));
    }

    @Test
    @DisplayName("POST /equipos/{id}/mantenimientos: las notas son opcionales")
    void notasOpcionales() throws Exception {
        // Arrange
        when(registrarMantenimiento.registrar(any())).thenReturn(Datos.mantenimientoView(5L, 3L, "300.0"));

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
        when(registrarMantenimiento.registrar(any())).thenThrow(new EquipoNotFoundException(3L));

        // Act + Assert
        mockMvc.perform(post("/equipos/3/mantenimientos").header(HttpHeaders.AUTHORIZATION, token())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /equipos/{id}/mantenimientos: 200 con el historial")
    void historial() throws Exception {
        // Arrange
        when(consultarMantenimientos.listarPorEquipo(3L))
                .thenReturn(List.of(Datos.mantenimientoView(2L, 3L, "500.0"), Datos.mantenimientoView(1L, 3L, "250.0")));

        // Act + Assert
        mockMvc.perform(get("/equipos/3/mantenimientos").header(HttpHeaders.AUTHORIZATION, token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].horometroAlRegistrar").value(500.0));
    }

    @Test
    @DisplayName("GET /equipos/{id}/mantenimientos: 404 si el equipo no existe y 400 si el id es 0")
    void historialConErrores() throws Exception {
        // Arrange
        when(consultarMantenimientos.listarPorEquipo(9L)).thenThrow(new EquipoNotFoundException(9L));

        // Act + Assert
        mockMvc.perform(get("/equipos/9/mantenimientos").header(HttpHeaders.AUTHORIZATION, token()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/equipos/0/mantenimientos").header(HttpHeaders.AUTHORIZATION, token()))
                .andExpect(status().isBadRequest());
    }
}
