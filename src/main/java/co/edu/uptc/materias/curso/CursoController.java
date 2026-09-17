package co.edu.uptc.materias.curso;

import co.edu.uptc.materias.curso.dto.CursoPatchRequest;
import co.edu.uptc.materias.curso.dto.CursoRequest;
import co.edu.uptc.materias.curso.dto.CursoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * GET /api/materias/cursos/{id} es el endpoint que el Gateway invoca en el
 * paso 3 de la composición de /api/estudiantes/{id}/detalle (ver sección
 * 6.1 del enunciado del laboratorio).
 */
@RestController
@RequestMapping("/api/materias/cursos")
@RequiredArgsConstructor
@Tag(name = "Cursos", description = "CRUD de cursos (oferta real de una materia: horario, período, cupo)")
public class CursoController {

    private final CursoService service;

    @GetMapping
    @Operation(summary = "Listar cursos (paginado, ordenado y con filtros opcionales)")
    public ResponseEntity<Page<CursoResponse>> listar(
            @Parameter(description = "Número de página, comenzando desde 0") @RequestParam(defaultValue = "0") int pageNumber,

            @Parameter(description = "Cantidad de registros por página") @RequestParam(defaultValue = "20") int pageSize,

            @Parameter(description = "Campo por el cual ordenar") @RequestParam(defaultValue = "id") String sortBy,

            @Parameter(description = "Dirección del ordenamiento: asc o desc") @RequestParam(defaultValue = "asc") String sortDirection,

            @Parameter(description = "Filtrar por período exacto (ej. 2026-2)") @RequestParam(required = false) String periodo,

            @Parameter(description = "Filtrar por id de materia") @RequestParam(required = false) Long materiaId,

            @Parameter(description = "Filtrar por id de docente") @RequestParam(required = false) Long docenteId,

            @Parameter(description = "Cupo mínimo (mayor o igual a)") @RequestParam(required = false) Integer cupoMin) {

        Sort.Direction direction = "desc".equalsIgnoreCase(sortDirection)
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(direction, sortBy));

        boolean hayFiltros = periodo != null || materiaId != null || docenteId != null || cupoMin != null;
        Page<Curso> resultado = hayFiltros
                ? service.listarConFiltros(periodo, materiaId, docenteId, cupoMin, pageable)
                : service.listar(pageable);

        return ResponseEntity.ok(resultado.map(CursoResponse::from));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un curso por id (materia y docente ya resueltos)")
    public ResponseEntity<CursoResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(CursoResponse.from(service.obtener(id)));
    }

    @PostMapping
    @Operation(summary = "Crear un nuevo curso (referencia materiaId y docenteId existentes)")
    public ResponseEntity<CursoResponse> crear(@Valid @RequestBody CursoRequest request) {
        Curso creado = service.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(CursoResponse.from(creado));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Reemplazar completamente un curso existente")
    public ResponseEntity<CursoResponse> reemplazar(@PathVariable Long id, @Valid @RequestBody CursoRequest request) {
        return ResponseEntity.ok(CursoResponse.from(service.reemplazar(id, request)));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Actualizar parcialmente un curso")
    public ResponseEntity<CursoResponse> actualizarParcial(@PathVariable Long id, @RequestBody CursoPatchRequest patch) {
        return ResponseEntity.ok(CursoResponse.from(service.actualizarParcial(id, patch)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar un curso")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
