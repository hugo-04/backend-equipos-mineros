package com.mineria.equipos.shared.pagination;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.mapstruct.Mapping;

/**
 * Para pasar un {@code Page} de Spring Data a la página del contrato ({@code PaginaXxxResponse}) sin repetir el
 * mapeo en cada mapper. Casi todo se copia por nombre; lo único distinto es que el número de página de Spring
 * se llama {@code number} y en el contrato es {@code page}. Así la API nunca serializa el {@code Page} de
 * Spring tal cual, porque su JSON no es estable entre versiones.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
@Mapping(target = "page", source = "number")
public @interface MapeoDePagina {
}
