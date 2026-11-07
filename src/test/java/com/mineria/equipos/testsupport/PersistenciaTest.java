package com.mineria.equipos.testsupport;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.mineria.equipos.infrastructure.adapter.out.persistence.EquipoPersistenceAdapter;
import com.mineria.equipos.infrastructure.adapter.out.persistence.MantenimientoPersistenceAdapter;
import com.mineria.equipos.infrastructure.adapter.out.persistence.TipoEquipoPersistenceAdapter;
import com.mineria.equipos.infrastructure.adapter.out.persistence.UsuarioPersistenceAdapter;
import com.mineria.equipos.infrastructure.adapter.out.persistence.mapper.EquipoPersistenceMapperImpl;
import com.mineria.equipos.infrastructure.adapter.out.persistence.mapper.MantenimientoPersistenceMapperImpl;
import com.mineria.equipos.infrastructure.adapter.out.persistence.mapper.TipoEquipoPersistenceMapperImpl;
import com.mineria.equipos.infrastructure.adapter.out.persistence.mapper.UsuarioPersistenceMapperImpl;

/**
 * Prueba de integración de la capa de persistencia: levanta solo JPA + Flyway sobre H2 en modo MySQL
 * (con las migraciones reales) y los adaptadores con sus mappers.
 * Se eligió H2 en vez de Testcontainers para que las pruebas corran sin Docker, en cualquier máquina y en CI.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({
        EquipoPersistenceAdapter.class, TipoEquipoPersistenceAdapter.class,
        MantenimientoPersistenceAdapter.class, UsuarioPersistenceAdapter.class,
        EquipoPersistenceMapperImpl.class, TipoEquipoPersistenceMapperImpl.class,
        MantenimientoPersistenceMapperImpl.class, UsuarioPersistenceMapperImpl.class
})
public @interface PersistenciaTest {
}
