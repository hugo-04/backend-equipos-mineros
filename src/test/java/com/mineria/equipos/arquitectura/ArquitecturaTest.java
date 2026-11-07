package com.mineria.equipos.arquitectura;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import java.util.List;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import jakarta.persistence.Entity;

/**
 * Pruebas de arquitectura del monolito modular. Hacen fallar el build si alguien rompe las reglas:
 * un módulo solo usa a otro a través de la interfaz de su {@code service}; no hay ciclos entre módulos;
 * el controller no toca repositorios ni entidades; y las excepciones viven solo en {@code shared/exception}.
 * Solo se analiza el código de producción. La visibilidad de Java no alcanza (las clases son públicas
 * porque viven en subpaquetes distintos), por eso el control de acceso se hace aquí.
 */
@AnalyzeClasses(packages = "com.mineria.equipos", importOptions = ImportOption.DoNotIncludeTests.class)
class ArquitecturaTest {

    private static final String BASE = "com.mineria.equipos.";
    private static final List<String> MODULOS = List.of("equipo", "tipo", "mantenimiento", "auth");

    // ---------- Reglas entre módulos ----------

    /**
     * Un módulo solo puede usar de otro la interfaz de su service. Nunca su controller, la implementación
     * del service, el repositorio, la entidad, la proyección, el mapper, la política ni su seguridad.
     */
    @ArchTest
    static void unModuloSoloUsaLaInterfazDeServiceDeOtro(JavaClasses clases) {
        for (String usuario : MODULOS) {
            for (String dueno : MODULOS) {
                if (usuario.equals(dueno)) {
                    continue;
                }
                noClasses()
                        .that().resideInAPackage(BASE + usuario + "..")
                        .should().dependOnClassesThat().resideInAnyPackage(
                                BASE + dueno + ".controller..", BASE + dueno + ".service.impl..",
                                BASE + dueno + ".repository..", BASE + dueno + ".entity..",
                                BASE + dueno + ".mapper..", BASE + dueno + ".policy..", BASE + dueno + ".security..")
                        .because("'" + usuario + "' solo puede usar '" + dueno + "' a través de la interfaz de su service")
                        .check(clases);
            }
        }
    }

    @ArchTest
    static final ArchRule sinCiclosEntreModulos = slices()
            .matching(BASE + "(*)..")
            .should().beFreeOfCycles()
            .because("las dependencias entre módulos deben ir en un solo sentido");

    @ArchTest
    static final ArchRule equipoNoDependeDeMantenimiento = noClasses()
            .that().resideInAPackage(BASE + "equipo..")
            .should().dependOnClassesThat().resideInAPackage(BASE + "mantenimiento..")
            .because("mantenimiento depende de equipo, nunca al revés");

    @ArchTest
    static final ArchRule sharedNoDependeDeLosModulos = noClasses()
            .that().resideInAPackage(BASE + "shared..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    BASE + "equipo..", BASE + "tipo..", BASE + "mantenimiento..", BASE + "auth..")
            .because("shared es común a todos y no conoce a ningún módulo");

    // ---------- Reglas de capas dentro de un módulo ----------

    @ArchTest
    static final ArchRule controllersNoAccedenAPersistenciaNiLogica = noClasses()
            .that().haveSimpleNameEndingWith("Controller")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..repository..", "..entity..", "..mapper..", "..service.impl..", "org.springframework.data..")
            .because("el controller depende solo de interfaces de service: nunca de repositorios, entidades ni mappers");

    @ArchTest
    static final ArchRule controllersEstanEnSuCarpeta = classes()
            .that().haveSimpleNameEndingWith("Controller")
            .should().resideInAnyPackage("..controller..", "..shared.health..")
            .because("los controllers viven en la carpeta controller de su módulo");

    @ArchTest
    static final ArchRule repositoriosSonInterfaces = classes()
            .that().haveSimpleNameEndingWith("Repository")
            .should().beInterfaces().andShould().resideInAPackage("..repository..")
            .because("los repositorios son interfaces de Spring Data en la carpeta repository");

    @ArchTest
    static final ArchRule proyeccionesSonInterfaces = classes()
            .that().resideInAPackage("..repository.projection..")
            .should().beInterfaces()
            .because("las proyecciones son interfaces de solo lectura");

    @ArchTest
    static final ArchRule entidadesVivenEnEntity = classes()
            .that().areAnnotatedWith(Entity.class)
            .should().resideInAPackage("..entity..")
            .because("las entidades JPA viven en la carpeta entity de su módulo");

    @ArchTest
    static final ArchRule serviciosTienenInterfazEImplementacion = classes()
            .that().resideInAPackage("..service.impl..")
            .should().haveSimpleNameEndingWith("ServiceImpl")
            .andShould().bePublic()
            .because("cada implementación vive en service/impl y se llama XxxServiceImpl");

    @ArchTest
    static final ArchRule interfacesDeServiceSonInterfaces = classes()
            .that().resideInAPackage("..service")
            .should().beInterfaces()
            .because("lo que vive en service (sin impl) es la interfaz que usan controller y otros módulos");

    @ArchTest
    static final ArchRule laPersistenciaNoConoceALaLogica = noClasses()
            .that().resideInAnyPackage("..repository..", "..entity..")
            .should().dependOnClassesThat().resideInAnyPackage("..service..", "..controller..")
            .because("las capas de abajo no conocen a las de arriba");

    // ---------- Excepciones ----------

    @ArchTest
    static final ArchRule excepcionesSoloEnSharedException = classes()
            .that().areAssignableTo(Throwable.class)
            .should().resideInAPackage("..shared.exception..")
            .because("todas las excepciones viven en shared/exception, no repartidas por módulo");
}
