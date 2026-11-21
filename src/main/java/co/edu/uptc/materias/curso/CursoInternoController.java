package co.edu.uptc.materias.curso;

import co.edu.uptc.materias.curso.dto.CursoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints internos que solo usa el módulo Inscripciones por la red privada.
 * El Gateway NO enruta /internal/**, así que no son accesibles desde internet.
 */
@RestController
@RequestMapping("/internal/cursos/{id}")
@RequiredArgsConstructor
@Tag(name = "Cursos (interno)", description = "Reserva y liberación atómica de cupos, solo para Inscripciones")
public class CursoInternoController {

    private final CursoService service;

    @PostMapping("/reservar")
    @Operation(summary = "Descontar un cupo (200), o 404 / 422 CURSO_INACTIVO / 409 SIN_CUPO")
    public ResponseEntity<CursoResponse> reservar(@PathVariable Long id) {
        return ResponseEntity.ok(CursoResponse.from(service.reservarCupo(id)));
    }

    @PostMapping("/liberar")
    @Operation(summary = "Devolver un cupo (compensación)")
    public ResponseEntity<CursoResponse> liberar(@PathVariable Long id) {
        return ResponseEntity.ok(CursoResponse.from(service.liberarCupo(id)));
    }
}
