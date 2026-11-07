package com.mineria.equipos.testsupport;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;

/**
 * Controller solo para pruebas: cada endpoint lanza una excepción distinta para comprobar
 * cómo la traduce el GlobalExceptionHandler. Vive fuera de la clase de test porque Spring Boot
 * excluye del escaneo las clases anidadas dentro de tests.
 */
@RestController
@RequestMapping("/pruebas")
public class ErroresDePruebaController {

    @GetMapping("/integridad")
    void integridad() {
        throw new DataIntegrityViolationException("detalle interno de SQL", new IllegalStateException("fk_secreta"));
    }

    @GetMapping("/acceso-denegado")
    void accesoDenegado() {
        throw new AccessDeniedException("sin permiso");
    }

    @GetMapping("/autenticacion")
    void autenticacion() {
        throw new BadCredentialsException("credenciales");
    }

    @GetMapping("/inesperado")
    void inesperado() {
        throw new IllegalStateException("clave interna: secreto-123");
    }

    @GetMapping("/restricciones")
    void restricciones() {
        ConstraintViolation<?> violacion = mock(ConstraintViolation.class);
        Path ruta = mock(Path.class);
        when(ruta.toString()).thenReturn("obtenerEquipo.id");
        when(violacion.getPropertyPath()).thenReturn(ruta);
        when(violacion.getMessage()).thenReturn("debe ser mayor que o igual a 1");
        throw new ConstraintViolationException(Set.of(violacion));
    }

    @PostMapping(value = "/solo-json", consumes = MediaType.APPLICATION_JSON_VALUE)
    void soloJson(@RequestBody String cuerpo) {
    }
}
