package co.edu.uptc.materias.materia;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Catálogo de materias: nombre, créditos y programa al que pertenece.
 * Es la entidad raíz del módulo (no depende de Docente ni de Curso).
 */
@Entity
@Table(name = "materias")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Materia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    @Column(nullable = false)
    private String nombre;

    @NotBlank(message = "El código es obligatorio")
    @Column(nullable = false, unique = true)
    private String codigo;

    @NotNull(message = "Los créditos son obligatorios")
    @Min(value = 1, message = "Los créditos deben ser al menos 1")
    @Max(value = 10, message = "Los créditos no deben superar 10")
    private Integer creditos;

    @NotBlank(message = "El programa académico es obligatorio")
    private String programa;

    /**
     * Código corto del programa (ISIS, IIND, ICIV, ADEM, CPUB, DERE, TGRE).
     * Es el dato estable que usan el Gateway y el Frontend para validar/filtrar.
     */
    @NotBlank(message = "El código del programa es obligatorio")
    @Column(length = 20)
    private String programaCodigo;

    /** Semestre del pensum (1 = más fácil, 10 = más difícil). */
    @NotNull(message = "El semestre es obligatorio")
    @Min(value = 1, message = "El semestre debe estar entre 1 y 10")
    @Max(value = 10, message = "El semestre debe estar entre 1 y 10")
    private Integer semestre;
}
