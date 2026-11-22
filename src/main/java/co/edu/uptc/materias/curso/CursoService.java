package co.edu.uptc.materias.curso;

import co.edu.uptc.materias.curso.dto.CursoPatchRequest;
import co.edu.uptc.materias.curso.dto.CursoRequest;
import co.edu.uptc.materias.docente.Docente;
import co.edu.uptc.materias.docente.DocenteRepository;
import co.edu.uptc.materias.exception.ConflictException;
import co.edu.uptc.materias.exception.ResourceNotFoundException;
import co.edu.uptc.materias.exception.UnprocessableException;
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
     * - programaCodigo: cursos cuya materia pertenece a ese programa (ISIS, IIND, ...).
     * - semestre: cursos cuya materia está en ese semestre del pensum.
     * - cupoMin: cupo mayor o igual al valor dado.
     */
    @Transactional(readOnly = true)
    public Page<Curso> listarConFiltros(String periodo, Long materiaId, Long docenteId, Integer cupoMin,
            String programaCodigo, Integer semestre, Pageable pageable) {
        Specification<Curso> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (periodo != null && !periodo.isBlank()) {
                predicates.add(cb.equal(root.get("periodo"), periodo));
            }

            if (materiaId != null) {
                predicates.add(cb.equal(root.get("materia").get("id"), materiaId));
            }

            if (programaCodigo != null && !programaCodigo.isBlank()) {
                predicates.add(cb.equal(
                        cb.upper(root.get("materia").get("programaCodigo")),
                        programaCodigo.toUpperCase()));
            }

            if (semestre != null) {
                predicates.add(cb.equal(root.get("materia").get("semestre"), semestre));
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
        cambiarCupo(existente, request.cupo());
        return repository.save(existente);
    }

    @Transactional
    public Curso actualizarParcial(Long id, CursoPatchRequest patch) {
        Curso existente = obtener(id);
        if (patch.materiaId() != null) existente.setMateria(obtenerMateria(patch.materiaId()));
        if (patch.docenteId() != null) existente.setDocente(obtenerDocente(patch.docenteId()));
        if (patch.horario() != null) existente.setHorario(patch.horario());
        if (patch.periodo() != null) existente.setPeriodo(patch.periodo());
        if (patch.cupo() != null) cambiarCupo(existente, patch.cupo());
        return repository.save(existente);
    }

    /**
     * A1: un curso NO se borra (las inscripciones de otro módulo lo siguen
     * referenciando y no hay FK que las proteja). Se marca INACTIVO y
     * Inscripciones rechaza con 422 las nuevas inscripciones a él.
     */
    @Transactional
    public void eliminar(Long id) {
        if (repository.inactivar(id) == 0) {
            throw ResourceNotFoundException.of("Curso", id);
        }
    }

    /**
     * A2: reserva un cupo con una sola sentencia condicional. Si no afectó
     * filas, se averigua el motivo solo para responder el código correcto:
     * 404 no existe, 422 INACTIVO, 409 sin cupo.
     */
    @Transactional
    public Curso reservarCupo(Long id) {
        if (repository.reservarCupo(id) == 0) {
            Curso curso = obtener(id);
            if (curso.getEstado() == EstadoCurso.INACTIVO) {
                throw new UnprocessableException("CURSO_INACTIVO", "El curso " + id + " está INACTIVO");
            }
            throw new ConflictException("SIN_CUPO", "El curso " + id + " no tiene cupos disponibles");
        }
        return obtener(id);
    }

    /** Compensación de una reserva (inscripción fallida o cancelada). */
    @Transactional
    public Curso liberarCupo(Long id) {
        repository.liberarCupo(id);
        return obtener(id);
    }

    /**
     * Cambiar el cupo total mueve los disponibles en la misma cantidad
     * (los ya inscritos se conservan). Si quedarían negativos, 409.
     */
    private void cambiarCupo(Curso curso, Integer nuevoCupo) {
        int anterior = curso.getCupo();
        int disponibles = curso.getCuposDisponibles() == null ? anterior : curso.getCuposDisponibles();
        int nuevosDisponibles = disponibles + (nuevoCupo - anterior);
        if (nuevosDisponibles < 0) {
            throw new ConflictException("CUPO_MENOR_QUE_INSCRITOS",
                    "El cupo no puede ser menor que los " + (anterior - disponibles) + " estudiantes ya inscritos");
        }
        curso.setCupo(nuevoCupo);
        curso.setCuposDisponibles(nuevosDisponibles);
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
