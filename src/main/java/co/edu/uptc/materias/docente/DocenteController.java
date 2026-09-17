package co.edu.uptc.materias.docente;

import co.edu.uptc.materias.docente.dto.DocentePatchRequest;
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

@RestController
@RequestMapping("/api/materias/docentes")
@RequiredArgsConstructor
@Tag(name = "Docentes", description = "CRUD de docentes")
public class DocenteController {

    private final DocenteService service;

    @GetMapping
    @Operation(summary = "Listar docentes (paginado, ordenado y con filtros opcionales)")
    public ResponseEntity<Page<Docente>> listar(
            @Parameter(description = "Número de página, comenzando desde 0") @RequestParam(defaultValue = "0") int pageNumber,

            @Parameter(description = "Cantidad de registros por página") @RequestParam(defaultValue = "20") int pageSize,

            @Parameter(description = "Campo por el cual ordenar") @RequestParam(defaultValue = "id") String sortBy,

            @Parameter(description = "Dirección del ordenamiento: asc o desc") @RequestParam(defaultValue = "asc") String sortDirection,

            @Parameter(description = "Filtrar por especialidad (coincidencia exacta, case-insensitive)") @RequestParam(required = false) String especialidad,

            @Parameter(description = "Texto a buscar en nombre, apellido o correo (parcial, case-insensitive)") @RequestParam(required = false) String search) {

        Sort.Direction direction = "desc".equalsIgnoreCase(sortDirection)
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(direction, sortBy));

        boolean hayFiltros = especialidad != null || search != null;
        Page<Docente> resultado = hayFiltros
                ? service.listarConFiltros(especialidad, search, pageable)
                : service.listar(pageable);

        return ResponseEntity.ok(resultado);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un docente por id")
    public ResponseEntity<Docente> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtener(id));
    }

    @PostMapping
    @Operation(summary = "Crear un nuevo docente")
    public ResponseEntity<Docente> crear(@Valid @RequestBody Docente docente) {
        Docente creado = service.crear(docente);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Reemplazar completamente un docente existente")
    public ResponseEntity<Docente> reemplazar(@PathVariable Long id, @Valid @RequestBody Docente docente) {
        return ResponseEntity.ok(service.reemplazar(id, docente));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Actualizar parcialmente un docente")
    public ResponseEntity<Docente> actualizarParcial(@PathVariable Long id, @RequestBody DocentePatchRequest patch) {
        return ResponseEntity.ok(service.actualizarParcial(id, patch));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar un docente")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
