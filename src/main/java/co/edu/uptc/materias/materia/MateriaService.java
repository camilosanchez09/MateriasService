package co.edu.uptc.materias.materia;

import co.edu.uptc.materias.archivo.ArchivoMateriaService;
import co.edu.uptc.materias.exception.ResourceNotFoundException;
import co.edu.uptc.materias.materia.dto.MateriaPatchRequest;
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
public class MateriaService {

    private final MateriaRepository repository;
    private final ArchivoMateriaService archivoService;

    @Transactional(readOnly = true)
    public Page<Materia> listar(Pageable pageable) {
        return repository.findAll(pageable);
    }

    /**
     * Filtros soportados (se combinan con AND, todos opcionales):
     * - programa: coincidencia exacta, case-insensitive.
     * - programaCodigo: código corto del programa (ISIS, IIND, ...), case-insensitive.
     * - search: texto parcial en nombre o código, case-insensitive.
     * - semestre: semestre exacto del pensum.
     * - creditosMin: créditos mayores o iguales al valor dado.
     */
    @Transactional(readOnly = true)
    public Page<Materia> listarConFiltros(String programa, String programaCodigo, String search,
            Integer semestre, Integer creditosMin, Pageable pageable) {
        Specification<Materia> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (programa != null && !programa.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("programa")), programa.toLowerCase()));
            }

            if (programaCodigo != null && !programaCodigo.isBlank()) {
                predicates.add(cb.equal(cb.upper(root.get("programaCodigo")), programaCodigo.toUpperCase()));
            }

            if (semestre != null) {
                predicates.add(cb.equal(root.get("semestre"), semestre));
            }

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.toLowerCase() + "%";
                Predicate nombreMatch = cb.like(cb.lower(root.get("nombre")), pattern);
                Predicate codigoMatch = cb.like(cb.lower(root.get("codigo")), pattern);
                predicates.add(cb.or(nombreMatch, codigoMatch));
            }

            if (creditosMin != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("creditos"), creditosMin));
            }

            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };

        return repository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public Materia obtener(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Materia", id));
    }

    @Transactional
    public Materia crear(Materia materia) {
        materia.setId(null); // el id siempre lo genera la BD
        return repository.save(materia);
    }

    @Transactional
    public Materia reemplazar(Long id, Materia datos) {
        Materia existente = obtener(id);
        existente.setNombre(datos.getNombre());
        existente.setCodigo(datos.getCodigo());
        existente.setCreditos(datos.getCreditos());
        existente.setPrograma(datos.getPrograma());
        existente.setProgramaCodigo(datos.getProgramaCodigo());
        existente.setSemestre(datos.getSemestre());
        return repository.save(existente);
    }

    @Transactional
    public Materia actualizarParcial(Long id, MateriaPatchRequest patch) {
        Materia existente = obtener(id);
        if (patch.nombre() != null) existente.setNombre(patch.nombre());
        if (patch.codigo() != null) existente.setCodigo(patch.codigo());
        if (patch.creditos() != null) existente.setCreditos(patch.creditos());
        if (patch.programa() != null) existente.setPrograma(patch.programa());
        if (patch.programaCodigo() != null) existente.setProgramaCodigo(patch.programaCodigo());
        if (patch.semestre() != null) existente.setSemestre(patch.semestre());
        return repository.save(existente);
    }

    @Transactional
    public void eliminar(Long id) {
        if (!repository.existsById(id)) {
            throw ResourceNotFoundException.of("Materia", id);
        }
        // Sus PDFs se borran del bucket solo si la eliminación confirma (si tiene cursos, falla y no se pierde nada).
        archivoService.eliminarTodosDeMateria(id);
        repository.deleteById(id);
    }
}
