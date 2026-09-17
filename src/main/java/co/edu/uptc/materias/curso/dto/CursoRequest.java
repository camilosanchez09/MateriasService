package co.edu.uptc.materias.curso.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CursoRequest(
        @NotNull(message = "El id de la materia es obligatorio") Long materiaId,
        @NotNull(message = "El id del docente es obligatorio") Long docenteId,
        @NotBlank(message = "El horario es obligatorio") String horario,
        @NotBlank(message = "El período es obligatorio") String periodo,
        @NotNull(message = "El cupo es obligatorio") @Min(value = 1, message = "El cupo debe ser al menos 1") Integer cupo
) {
}
