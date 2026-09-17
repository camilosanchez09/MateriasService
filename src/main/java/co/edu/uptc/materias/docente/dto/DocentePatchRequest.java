package co.edu.uptc.materias.docente.dto;

public record DocentePatchRequest(
        String nombre,
        String apellido,
        String correo,
        String especialidad
) {
}
