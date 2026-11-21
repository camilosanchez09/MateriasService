package co.edu.uptc.materias.curso;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CursoRepository extends JpaRepository<Curso, Long>, JpaSpecificationExecutor<Curso> {

    /**
     * A2 · Cupo atómico. El descuento se hace en UNA sola sentencia: la base
     * evalúa la condición y escribe bajo el mismo bloqueo de fila, así que dos
     * solicitudes simultáneas nunca pueden tomar el mismo último cupo.
     * 0 filas afectadas = no existe, está INACTIVO o no quedan cupos.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE cursos
               SET cupos_disponibles = cupos_disponibles - 1
             WHERE id = :id
               AND estado = 'ACTIVO'
               AND cupos_disponibles > 0
            """, nativeQuery = true)
    int reservarCupo(@Param("id") Long id);

    /** Compensación: devuelve un cupo sin pasar nunca del cupo total. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE cursos
               SET cupos_disponibles = cupos_disponibles + 1
             WHERE id = :id
               AND cupos_disponibles < cupo
            """, nativeQuery = true)
    int liberarCupo(@Param("id") Long id);

    /** A1: "borrar" = marcar INACTIVO. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE cursos SET estado = 'INACTIVO' WHERE id = :id", nativeQuery = true)
    int inactivar(@Param("id") Long id);
}
