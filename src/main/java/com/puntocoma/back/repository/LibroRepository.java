package com.puntocoma.back.repository;

import com.puntocoma.back.entity.Libro;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LibroRepository extends JpaRepository<Libro, Long>, JpaSpecificationExecutor<Libro> {

    Optional<Libro> findByIdAndActivoTrue(Long id);

    List<Libro> findByPublicadoPorIdAndActivoTrueOrderByFechaPublicacionDesc(Long usuarioId);

    @Query("select distinct l.genero from Libro l where l.activo = true order by l.genero")
    List<String> findGenerosActivos();

    @Query("select distinct l.autor from Libro l where l.activo = true order by l.autor")
    List<String> findAutoresActivos();

    long countByActivoTrue();

    @Query("select count(distinct l.autor) from Libro l where l.activo = true")
    long countAutoresActivos();

    List<Libro> findTop3ByActivoTrueOrderByFechaPublicacionDescIdDesc();

    /** Bloquea la fila para evitar que dos préstamos simultáneos dejen stock negativo. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from Libro l where l.id = :id and l.activo = true")
    Optional<Libro> findActivoParaActualizar(@Param("id") Long id);
}
