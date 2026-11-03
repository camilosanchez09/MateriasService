package co.edu.uptc.materias.archivo;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArchivoMateriaRepository extends JpaRepository<ArchivoMateria, Long> {

    List<ArchivoMateria> findByMateriaIdOrderBySubidoEnDescIdDesc(Long materiaId);

    Optional<ArchivoMateria> findByIdAndMateriaId(Long id, Long materiaId);

    @Query("select a.objectKey from ArchivoMateria a where a.materia.id = :materiaId")
    List<String> findObjectKeysByMateriaId(@Param("materiaId") Long materiaId);

    @Modifying
    @Query("delete from ArchivoMateria a where a.materia.id = :materiaId")
    void deleteByMateriaId(@Param("materiaId") Long materiaId);
}
