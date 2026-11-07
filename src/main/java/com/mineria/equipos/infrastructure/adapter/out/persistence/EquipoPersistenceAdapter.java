package com.mineria.equipos.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import com.mineria.equipos.application.port.out.EquipoQueryPort;
import com.mineria.equipos.application.port.out.EquipoRepositoryPort;
import com.mineria.equipos.application.view.EquipoView;
import com.mineria.equipos.shared.exception.CodigoDuplicadoException;
import com.mineria.equipos.domain.model.Equipo;
import com.mineria.equipos.infrastructure.adapter.out.persistence.mapper.EquipoPersistenceMapper;
import com.mineria.equipos.infrastructure.adapter.out.persistence.repository.EquipoJpaRepository;

import lombok.RequiredArgsConstructor;

/**
 * Adaptador que implementa los puertos de equipos con Spring Data JPA.
 * Escribe con el modelo de dominio y lee con proyecciones (views); las entidades JPA
 * no salen de este paquete.
 */
@Component
@RequiredArgsConstructor
public class EquipoPersistenceAdapter implements EquipoRepositoryPort, EquipoQueryPort {

    /** Nombre de la restricción única del código en la migración V2. */
    private static final String RESTRICCION_CODIGO_UNICO = "uk_equipo_codigo";

    private final EquipoJpaRepository repository;
    private final EquipoPersistenceMapper mapper;

    @Override
    public Optional<Equipo> buscarPorId(Long id) {
        return repository.buscarConTipo(id).map(mapper::toDomain);
    }

    @Override
    public Equipo guardar(Equipo equipo) {
        try {
            // saveAndFlush fuerza el INSERT/UPDATE ya, para detectar aquí un código repetido
            return mapper.toDomain(repository.saveAndFlush(mapper.toEntity(equipo)));
        } catch (DataIntegrityViolationException e) {
            // Dos altas simultáneas con el mismo código pasan el chequeo previo; la BD las frena
            if (violaCodigoUnico(e)) {
                throw new CodigoDuplicadoException(equipo.getCodigo());
            }
            throw e;
        }
    }

    @Override
    public boolean existePorId(Long id) {
        return repository.existsById(id);
    }

    @Override
    public boolean existePorCodigo(String codigo) {
        return repository.existsByCodigo(codigo);
    }

    @Override
    public boolean existePorCodigoDistintoDe(String codigo, Long id) {
        return repository.existsByCodigoAndIdNot(codigo, id);
    }

    @Override
    public void eliminarPorId(Long id) {
        repository.deleteById(id);
        repository.flush();
    }

    @Override
    public List<EquipoView> listar() {
        return repository.listarProyecciones().stream().map(mapper::toView).toList();
    }

    @Override
    public Optional<EquipoView> buscarVistaPorId(Long id) {
        return repository.buscarProyeccionPorId(id).map(mapper::toView);
    }

    private boolean violaCodigoUnico(DataIntegrityViolationException e) {
        return Optional.ofNullable(e.getMostSpecificCause().getMessage())
                .map(mensaje -> mensaje.toLowerCase(Locale.ROOT).contains(RESTRICCION_CODIGO_UNICO))
                .orElse(false);
    }
}
