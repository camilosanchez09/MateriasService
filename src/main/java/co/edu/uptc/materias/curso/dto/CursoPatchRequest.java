package co.edu.uptc.materias.curso.dto;

public record CursoPatchRequest(
        Long materiaId,
        Long docenteId,
        String horario,
        String periodo,
        Integer cupo
) {
}
