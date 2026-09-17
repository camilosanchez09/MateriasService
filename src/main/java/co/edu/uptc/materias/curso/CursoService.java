package co.edu.uptc.materias.curso;

import co.edu.uptc.materias.curso.dto.CursoPatchRequest;
import co.edu.uptc.materias.curso.dto.CursoRequest;
import co.edu.uptc.materias.docente.Docente;
import co.edu.uptc.materias.docente.DocenteRepository;
import co.edu.uptc.materias.exception.ResourceNotFoundException;
import co.edu.uptc.materias.materia.Materia;
import co.edu.uptc.materias.materia.MateriaRepository;
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
public class CursoService {

    private final CursoRepository repository;
    private final MateriaRepository materiaRepository;
    private final DocenteRepository docenteRepository;

    @Transactional(readOnly = true)
    public Page<Curso> listar(Pageable pageable) {
        return repository.findAll(pageable);
    }

    /**
     * Filtros soportados (AND, todos opcionales):
     * - periodo: coincidencia exacta (ej. "2026-2").
     * - materiaId / docenteId: cursos de una materia o docente puntual.
     * - cupoMin: cupo mayor o igual al valor dado.
     */
    @Transactional(readOnly = true)
    public Page<Curso> listarConFiltros(String periodo, Long materiaId, Long docenteId, Integer cupoMin, Pageable pageable) {
        Specification<Curso> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (periodo != null && !periodo.isBlank()) {
                predicates.add(cb.equal(root.get("periodo"), periodo));
            }

            if (materiaId != null) {
                predicates.add(cb.equal(root.get("materia").get("id"), materiaId));
            }

            if (docenteId != null) {
                predicates.add(cb.equal(root.get("docente").get("id"), docenteId));
            }

            if (cupoMin != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("cupo"), cupoMin));
            }

            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };

        return repository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public Curso obtener(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Curso", id));
    }

    @Transactional
    public Curso crear(CursoRequest request) {
        Curso curso = Curso.builder()
                .materia(obtenerMateria(request.materiaId()))
                .docente(obtenerDocente(request.docenteId()))
                .horario(request.horario())
                .periodo(request.periodo())
                .cupo(request.cupo())
                .build();
        return repository.save(curso);
    }

    @Transactional
    public Curso reemplazar(Long id, CursoRequest request) {
        Curso existente = obtener(id);
        existente.setMateria(obtenerMateria(request.materiaId()));
        existente.setDocente(obtenerDocente(request.docenteId()));
        existente.setHorario(request.horario());
        existente.setPeriodo(request.periodo());
        existente.setCupo(request.cupo());
        return repository.save(existente);
    }

    @Transactional
    public Curso actualizarParcial(Long id, CursoPatchRequest patch) {
        Curso existente = obtener(id);
        if (patch.materiaId() != null) existente.setMateria(obtenerMateria(patch.materiaId()));
        if (patch.docenteId() != null) existente.setDocente(obtenerDocente(patch.docenteId()));
        if (patch.horario() != null) existente.setHorario(patch.horario());
        if (patch.periodo() != null) existente.setPeriodo(patch.periodo());
        if (patch.cupo() != null) existente.setCupo(patch.cupo());
        return repository.save(existente);
    }

    @Transactional
    public void eliminar(Long id) {
        if (!repository.existsById(id)) {
            throw ResourceNotFoundException.of("Curso", id);
        }
        repository.deleteById(id);
    }

    private Materia obtenerMateria(Long materiaId) {
        return materiaRepository.findById(materiaId)
                .orElseThrow(() -> ResourceNotFoundException.of("Materia", materiaId));
    }

    private Docente obtenerDocente(Long docenteId) {
        return docenteRepository.findById(docenteId)
                .orElseThrow(() -> ResourceNotFoundException.of("Docente", docenteId));
    }
}
