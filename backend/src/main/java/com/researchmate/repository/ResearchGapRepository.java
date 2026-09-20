package com.researchmate.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.researchmate.entity.ResearchGap;

public interface ResearchGapRepository extends JpaRepository<ResearchGap, Long> {

    @Query("SELECT DISTINCT rg FROM ResearchGap rg " +
           "LEFT JOIN FETCH rg.evidenceList el " +
           "LEFT JOIN FETCH el.paper " +
           "WHERE rg.owner.id = :ownerId " +
           "ORDER BY rg.confidence DESC")
    List<ResearchGap> findByOwnerIdWithEvidence(@Param("ownerId") Long ownerId);

    Optional<ResearchGap> findByIdAndOwnerId(Long id, Long ownerId);

    void deleteByOwnerId(Long ownerId);
}
