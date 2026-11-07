package com.mineria.equipos.testsupport;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;


/**
 * Prueba de integración de la capa de persistencia: levanta solo JPA + Flyway sobre H2 en modo MySQL
 * (con las migraciones reales) y los repositorios de cada módulo.
 * Se eligió H2 en vez de Testcontainers para que las pruebas corran sin Docker, en cualquier máquina y en CI.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
public @interface PersistenciaTest {
}
