package com.researchmate.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.researchmate.entity.Paper;
import com.researchmate.entity.PaperStatus;

public interface PaperRepository extends JpaRepository<Paper, Long> {

    List<Paper> findByOwnerId(Long ownerId);

    Optional<Paper> findByIdAndOwnerId(Long paperId, Long ownerId);

    Page<Paper> findByOwnerId(Long ownerId, Pageable pageable);

    @Query("SELECT p FROM Paper p WHERE p.owner.id = :ownerId " +
           "AND (:search IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "     OR LOWER(p.abstractText) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:year IS NULL OR p.publicationYear = :year)")
    Page<Paper> findByOwnerIdWithFilters(
            @Param("ownerId") Long ownerId,
            @Param("search") String search,
            @Param("year") Integer year,
            Pageable pageable);

    long countByOwnerId(Long ownerId);

    long countByOwnerIdAndStatus(Long ownerId, PaperStatus status);
}
