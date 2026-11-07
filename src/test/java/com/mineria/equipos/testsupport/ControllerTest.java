package com.mineria.equipos.testsupport;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;
import org.springframework.test.context.ActiveProfiles;

import com.mineria.equipos.shared.exception.ErrorResponseFactory;
import com.mineria.equipos.shared.exception.GlobalExceptionHandler;
import com.mineria.equipos.shared.config.ApplicationConfig;
import com.mineria.equipos.shared.config.CorsConfig;
import com.mineria.equipos.auth.security.JwtService;
import com.mineria.equipos.auth.security.SecurityConfig;
import com.mineria.equipos.auth.security.SecurityErrorHandlers;

/**
 * Prueba de controllers con MockMvc: levanta solo la capa web real (controllers, mappers, seguridad JWT,
 * CORS y manejo de errores) y deja los casos de uso para simular con Mockito.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@WebMvcTest
@ActiveProfiles("test")
@Import({
        SecurityConfig.class, SecurityErrorHandlers.class, JwtService.class, CorsConfig.class,
        ApplicationConfig.class, ErrorResponseFactory.class, GlobalExceptionHandler.class
})
public @interface ControllerTest {

    @AliasFor(annotation = WebMvcTest.class, attribute = "controllers")
    Class<?>[] value() default {};
}
