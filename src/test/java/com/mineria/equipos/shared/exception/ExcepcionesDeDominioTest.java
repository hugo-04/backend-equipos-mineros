package com.mineria.equipos.shared.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ExcepcionesDeDominioTest {

    static Stream<Arguments> casos() {
        return Stream.of(
                Arguments.of(new EquipoNotFoundException(9L), "EQUIPO_NO_ENCONTRADO", "id 9"),
                Arguments.of(new TipoEquipoNotFoundException(4L), "TIPO_EQUIPO_NO_ENCONTRADO", "id 4"),
                Arguments.of(new CodigoDuplicadoException("CAM-001"), "CODIGO_DUPLICADO", "CAM-001"),
                Arguments.of(new HorometroInvalidoException("no puede bajar"), "HOROMETRO_INVALIDO", "no puede bajar"),
                Arguments.of(new CredencialesInvalidasException(), "CREDENCIALES_INVALIDAS", "incorrectos"));
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("casos")
    @DisplayName("cada excepción lleva su código interno y un mensaje claro")
    void codigoYMensaje(DomainException excepcion, String codigoEsperado, String fragmentoDelMensaje) {
        // Arrange: la excepción ya viene armada por el MethodSource

        // Act
        String codigo = excepcion.getCodigo();
        String mensaje = excepcion.getMessage();

        // Assert
        assertThat(codigo).isEqualTo(codigoEsperado);
        assertThat(mensaje).contains(fragmentoDelMensaje);
    }
}
