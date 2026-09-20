package com.researchmate.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.researchmate.entity.PaperAnalysis;

public interface PaperAnalysisRepository extends JpaRepository<PaperAnalysis, Long> {

    Optional<PaperAnalysis> findByPaperId(Long paperId);

    @Query("SELECT pa FROM PaperAnalysis pa WHERE pa.paper.id = :paperId AND pa.paper.owner.id = :ownerId")
    Optional<PaperAnalysis> findByPaperIdAndOwnerId(@Param("paperId") Long paperId, @Param("ownerId") Long ownerId);

    void deleteByPaperId(Long paperId);
}
