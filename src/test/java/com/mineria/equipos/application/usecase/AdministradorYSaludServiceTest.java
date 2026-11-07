package com.mineria.equipos.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mineria.equipos.application.port.out.PasswordHasherPort;
import com.mineria.equipos.application.port.out.SaludBaseDatosPort;
import com.mineria.equipos.application.port.out.UsuarioRepositoryPort;
import com.mineria.equipos.application.view.EstadoSalud;
import com.mineria.equipos.domain.model.Rol;
import com.mineria.equipos.domain.model.Usuario;
import com.mineria.equipos.testsupport.Datos;

/** Pruebas del administrador inicial y del health check. */
@ExtendWith(MockitoExtension.class)
class AdministradorYSaludServiceTest {

    @Mock
    private UsuarioRepositoryPort usuarioRepository;
    @Mock
    private PasswordHasherPort passwordHasher;
    @Mock
    private SaludBaseDatosPort saludBaseDatos;

    @Test
    @DisplayName("crea el administrador con la clave hasheada cuando no existe")
    void creaAlAdministrador() {
        // Arrange
        when(usuarioRepository.buscarPorUsername("admin")).thenReturn(Optional.empty());
        when(passwordHasher.hashear("clave")).thenReturn("$2a$hasheado");

        // Act
        boolean creado = new AsegurarAdministradorService(usuarioRepository, passwordHasher).asegurar("admin", "clave");

        // Assert
        assertThat(creado).isTrue();
        ArgumentCaptor<Usuario> guardado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).guardar(guardado.capture());
        assertThat(guardado.getValue().getPasswordHash()).isEqualTo("$2a$hasheado");
        assertThat(guardado.getValue().getRol()).isEqualTo(Rol.ADMIN);
    }

    @Test
    @DisplayName("si el administrador ya existe no lo toca (idempotente)")
    void noRecreaAlAdministrador() {
        // Arrange
        when(usuarioRepository.buscarPorUsername("admin")).thenReturn(Optional.of(Datos.admin()));

        // Act
        boolean creado = new AsegurarAdministradorService(usuarioRepository, passwordHasher).asegurar("admin", "otra");

        // Assert
        assertThat(creado).isFalse();
        verify(usuarioRepository, never()).guardar(any());
    }

    @Test
    @DisplayName("salud: UP cuando la base de datos responde")
    void saludUp() {
        // Arrange
        when(saludBaseDatos.estaDisponible()).thenReturn(true);

        // Act
        EstadoSalud estado = new VerificarSaludService(saludBaseDatos).verificar();

        // Assert
        assertThat(estado).isEqualTo(EstadoSalud.UP);
    }

    @Test
    @DisplayName("salud: DOWN cuando la base de datos no responde")
    void saludDown() {
        // Arrange
        when(saludBaseDatos.estaDisponible()).thenReturn(false);

        // Act
        EstadoSalud estado = new VerificarSaludService(saludBaseDatos).verificar();

        // Assert
        assertThat(estado).isEqualTo(EstadoSalud.DOWN);
    }
}
