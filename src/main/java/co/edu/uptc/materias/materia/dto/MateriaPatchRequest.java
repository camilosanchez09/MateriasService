package co.edu.uptc.materias.materia.dto;

public record MateriaPatchRequest(
        String nombre,
        String codigo,
        Integer creditos,
        String programa
) {
}
