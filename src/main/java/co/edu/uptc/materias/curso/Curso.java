package co.edu.uptc.materias.curso;

import co.edu.uptc.materias.docente.Docente;
import co.edu.uptc.materias.materia.Materia;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Curso = la oferta real de una Materia en un período dictado por un
 * Docente (horario, período, cupo). Curso N:1 Materia y Curso N:1 Docente
 * son relaciones reales de base de datos porque las tres entidades viven
 * en el mismo módulo / misma base de datos — esto es distinto de un id
 * "externo" como estudianteId en Inscripciones, que nunca es una FK real
 * porque Estudiantes vive en otro módulo con su propia base de datos.
 */
@Entity
@Table(name = "cursos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Curso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "La materia es obligatoria")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "materia_id", nullable = false)
    private Materia materia;

    @NotNull(message = "El docente es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "docente_id", nullable = false)
    private Docente docente;

    @NotBlank(message = "El horario es obligatorio")
    @Column(nullable = false)
    private String horario;

    @NotBlank(message = "El período es obligatorio")
    @Column(nullable = false)
    private String periodo;

    @NotNull(message = "El cupo es obligatorio")
    @Min(value = 1, message = "El cupo debe ser al menos 1")
    @Column(nullable = false)
    private Integer cupo;
}
