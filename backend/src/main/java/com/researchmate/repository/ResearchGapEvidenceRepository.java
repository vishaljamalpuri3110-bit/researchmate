package com.researchmate.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.researchmate.entity.ResearchGapEvidence;

public interface ResearchGapEvidenceRepository extends JpaRepository<ResearchGapEvidence, Long> {

    List<ResearchGapEvidence> findByResearchGapId(Long researchGapId);

    @Modifying
    @Query("DELETE FROM ResearchGapEvidence rge WHERE rge.paper.id = :paperId")
    void deleteByPaperId(@Param("paperId") Long paperId);
}
