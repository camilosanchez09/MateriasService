package co.edu.uptc.materias.curso.dto;

import co.edu.uptc.materias.curso.Curso;

/**
 * Representación de salida de un Curso. Incluye el nombre de la materia
 * (materiaNombre) resuelto internamente vía join dentro de este mismo
 * módulo/base de datos — no es un dato duplicado guardado en la tabla,
 * solo se calcula al leer. docenteId queda como id "plano" para que sea
 * el Gateway quien, si lo necesita, pida el docente completo al módulo
 * Materias (paso 4 del endpoint de composición /detalle).
 */
public record CursoResponse(
        Long id,
        Long materiaId,
        String materiaNombre,
        Long docenteId,
        String docenteNombre,
        String horario,
        String periodo,
        Integer cupo
) {
    public static CursoResponse from(Curso curso) {
        return new CursoResponse(
                curso.getId(),
                curso.getMateria().getId(),
                curso.getMateria().getNombre(),
                curso.getDocente().getId(),
                curso.getDocente().getNombre() + " " + curso.getDocente().getApellido(),
                curso.getHorario(),
                curso.getPeriodo(),
                curso.getCupo()
        );
    }
}
