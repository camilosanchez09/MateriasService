package co.edu.uptc.materias.materia;

import co.edu.uptc.materias.materia.dto.MateriaPatchRequest;
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
 * Recurso raíz del módulo (montado bajo el prefijo /api/materias que el
 * Gateway enruta hacia este servicio). Los sub-recursos Docente y Curso
 * viven bajo /api/materias/docentes y /api/materias/cursos respectivamente,
 * para que el mismo prefijo cubra todo el módulo.
 */
@RestController
@RequestMapping("/api/materias")
@RequiredArgsConstructor
@Tag(name = "Materias", description = "CRUD del catálogo de materias")
public class MateriaController {

    private final MateriaService service;

    @GetMapping
    @Operation(summary = "Listar materias (paginado, ordenado y con filtros opcionales)")
    public ResponseEntity<Page<Materia>> listar(
            @Parameter(description = "Número de página, comenzando desde 0") @RequestParam(defaultValue = "0") int pageNumber,

            @Parameter(description = "Cantidad de registros por página") @RequestParam(defaultValue = "20") int pageSize,

            @Parameter(description = "Campo por el cual ordenar") @RequestParam(defaultValue = "id") String sortBy,

            @Parameter(description = "Dirección del ordenamiento: asc o desc") @RequestParam(defaultValue = "asc") String sortDirection,

            @Parameter(description = "Filtrar por programa académico (coincidencia exacta, case-insensitive)") @RequestParam(required = false) String programa,

            @Parameter(description = "Texto a buscar en nombre o código (parcial, case-insensitive)") @RequestParam(required = false) String search,

            @Parameter(description = "Créditos mínimos (mayor o igual a)") @RequestParam(required = false) Integer creditosMin) {

        Sort.Direction direction = "desc".equalsIgnoreCase(sortDirection)
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(direction, sortBy));

        boolean hayFiltros = programa != null || search != null || creditosMin != null;
        Page<Materia> resultado = hayFiltros
                ? service.listarConFiltros(programa, search, creditosMin, pageable)
                : service.listar(pageable);

        return ResponseEntity.ok(resultado);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener una materia por id")
    public ResponseEntity<Materia> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtener(id));
    }

    @PostMapping
    @Operation(summary = "Crear una nueva materia")
    public ResponseEntity<Materia> crear(@Valid @RequestBody Materia materia) {
        Materia creada = service.crear(materia);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Reemplazar completamente una materia existente")
    public ResponseEntity<Materia> reemplazar(@PathVariable Long id, @Valid @RequestBody Materia materia) {
        return ResponseEntity.ok(service.reemplazar(id, materia));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Actualizar parcialmente una materia")
    public ResponseEntity<Materia> actualizarParcial(@PathVariable Long id, @RequestBody MateriaPatchRequest patch) {
        return ResponseEntity.ok(service.actualizarParcial(id, patch));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar una materia")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
