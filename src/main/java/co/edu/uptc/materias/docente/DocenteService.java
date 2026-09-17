package co.edu.uptc.materias.docente;

import co.edu.uptc.materias.docente.dto.DocentePatchRequest;
import co.edu.uptc.materias.exception.ResourceNotFoundException;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DocenteService {

    private final DocenteRepository repository;

    @Transactional(readOnly = true)
    public Page<Docente> listar(Pageable pageable) {
        return repository.findAll(pageable);
    }

    /**
     * Filtros soportados (AND, todos opcionales):
     * - especialidad: coincidencia exacta, case-insensitive.
     * - search: texto parcial en nombre, apellido o correo.
     */
    @Transactional(readOnly = true)
    public Page<Docente> listarConFiltros(String especialidad, String search, Pageable pageable) {
        Specification<Docente> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (especialidad != null && !especialidad.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("especialidad")), especialidad.toLowerCase()));
            }

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.toLowerCase() + "%";
                Predicate nombreMatch = cb.like(cb.lower(root.get("nombre")), pattern);
                Predicate apellidoMatch = cb.like(cb.lower(root.get("apellido")), pattern);
                Predicate correoMatch = cb.like(cb.lower(root.get("correo")), pattern);
                predicates.add(cb.or(nombreMatch, apellidoMatch, correoMatch));
            }

            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };

        return repository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public Docente obtener(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Docente", id));
    }

    @Transactional
    public Docente crear(Docente docente) {
        docente.setId(null);
        return repository.save(docente);
    }

    @Transactional
    public Docente reemplazar(Long id, Docente datos) {
        Docente existente = obtener(id);
        existente.setNombre(datos.getNombre());
        existente.setApellido(datos.getApellido());
        existente.setCorreo(datos.getCorreo());
        existente.setEspecialidad(datos.getEspecialidad());
        return repository.save(existente);
    }

    @Transactional
    public Docente actualizarParcial(Long id, DocentePatchRequest patch) {
        Docente existente = obtener(id);
        if (patch.nombre() != null) existente.setNombre(patch.nombre());
        if (patch.apellido() != null) existente.setApellido(patch.apellido());
        if (patch.correo() != null) existente.setCorreo(patch.correo());
        if (patch.especialidad() != null) existente.setEspecialidad(patch.especialidad());
        return repository.save(existente);
    }

    @Transactional
    public void eliminar(Long id) {
        if (!repository.existsById(id)) {
            throw ResourceNotFoundException.of("Docente", id);
        }
        repository.deleteById(id);
    }
}
