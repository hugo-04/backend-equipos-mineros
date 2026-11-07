package com.mineria.equipos.shared.pagination;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.mapstruct.Mapping;

/**
 * Anotación compuesta de MapStruct para convertir un {@code Page<Proyección>} de Spring Data en la página del
 * contrato ({@code PaginaXxxResponse}). Evita repetir en cada mapper cómo se copian los datos de paginación:
 * {@code content}, {@code size}, {@code totalElements}, {@code totalPages}, {@code first} y {@code last} se
 * copian por nombre, y el número de página de Spring (que se llama {@code number}) pasa a {@code page}.
 * Así la API nunca serializa el {@code Page} de Spring tal cual (su JSON no es estable), sino la clase del contrato.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
@Mapping(target = "page", source = "number")
public @interface MapeoDePagina {
}
