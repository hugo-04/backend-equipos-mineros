package com.mineria.equipos.arquitectura;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Pruebas de arquitectura: hacen fallar el build si alguien rompe la regla de dependencia
 * (infrastructure -> application -> domain). Solo se analiza el código de producción.
 */
@AnalyzeClasses(packages = "com.mineria.equipos", importOptions = ImportOption.DoNotIncludeTests.class)
class ArquitecturaTest {

    @ArchTest
    static final ArchRule domainNoConoceFrameworks = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..", "jakarta.persistence..", "jakarta.servlet..", "jakarta.validation..",
                    "org.hibernate..", "com.fasterxml.jackson..", "io.swagger..", "org.openapitools..")
            .because("el dominio debe ser Java puro: sin Spring, JPA ni web");

    @ArchTest
    static final ArchRule domainNoDependeDeOtrasCapas = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage("..application..", "..infrastructure..")
            .because("las dependencias apuntan hacia adentro");

    @ArchTest
    static final ArchRule applicationNoConoceInfrastructure = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
            .because("la aplicación solo conoce el dominio y sus propios puertos");

    @ArchTest
    static final ArchRule applicationNoConoceJpaNiHttp = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "jakarta.persistence..", "org.hibernate..", "org.springframework.data..",
                    "org.springframework.web..", "org.springframework.http..", "jakarta.servlet..")
            .because("los casos de uso no saben de persistencia ni de HTTP");

    @ArchTest
    static final ArchRule controllersNoAccedenARepositorios = noClasses()
            .that().haveSimpleNameEndingWith("Controller")
            .should().dependOnClassesThat().resideInAnyPackage("..adapter.out..", "org.springframework.data..")
            .orShould().dependOnClassesThat().haveSimpleNameEndingWith("Repository")
            .because("los controllers solo hablan con los puertos de entrada (casos de uso)");

    @ArchTest
    static final ArchRule controllersSoloDependenDeCasosDeUso = noClasses()
            .that().haveSimpleNameEndingWith("Controller")
            .should().dependOnClassesThat().resideInAPackage("..application.usecase..")
            .because("el controller depende de las interfaces de port.in, nunca de sus implementaciones");

    @ArchTest
    static final ArchRule entidadesJpaSoloEnPersistencia = noClasses()
            .that().resideOutsideOfPackage("..adapter.out.persistence..")
            .should().dependOnClassesThat().resideInAPackage("..adapter.out.persistence.entity..")
            .because("las entidades JPA no salen del adaptador de persistencia");

    @ArchTest
    static final ArchRule adaptadoresDeEntradaNoConocenLosDeSalida = noClasses()
            .that().resideInAPackage("..adapter.in..")
            .should().dependOnClassesThat().resideInAPackage("..adapter.out..")
            .because("los adaptadores no se hablan entre sí: se comunican por los puertos");

    @ArchTest
    static final ArchRule loDeSalidaImplementaPuertos = classes()
            .that().resideInAPackage("..adapter.out.persistence").and().haveSimpleNameEndingWith("Adapter")
            .should().dependOnClassesThat().resideInAPackage("..application.port.out..")
            .because("cada adaptador de persistencia implementa un puerto de salida");

    @ArchTest
    static final ArchRule puertosSonInterfaces = classes()
            .that().resideInAnyPackage("..application.port..").and().areTopLevelClasses()
            .should().beInterfaces()
            .because("los puertos son contratos, no implementaciones (los Comando anidados son records de datos)");

    @ArchTest
    static final ArchRule casosDeUsoImplementanUnPuertoDeEntrada = classes()
            .that().resideInAPackage("..application.usecase..").and().haveSimpleNameEndingWith("Service")
            .should().dependOnClassesThat().resideInAPackage("..application.port.in..")
            .because("cada caso de uso implementa su interfaz de port.in");

    @ArchTest
    static final ArchRule capasEnOrden = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("Dominio").definedBy("..domain..")
            .layer("Aplicacion").definedBy("..application..")
            .layer("Infraestructura").definedBy("..infrastructure..")
            .whereLayer("Infraestructura").mayNotBeAccessedByAnyLayer()
            .whereLayer("Aplicacion").mayOnlyBeAccessedByLayers("Infraestructura")
            .whereLayer("Dominio").mayOnlyBeAccessedByLayers("Aplicacion", "Infraestructura");
}
