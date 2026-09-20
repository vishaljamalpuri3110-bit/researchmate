package com.researchmate.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.researchmate.entity.PaperSimilarity;
import com.researchmate.entity.SimilarityMethod;

public interface PaperSimilarityRepository extends JpaRepository<PaperSimilarity, Long> {

    @Query("SELECT ps FROM PaperSimilarity ps " +
           "JOIN FETCH ps.targetPaper tp " +
           "WHERE ps.sourcePaper.id = :sourceId AND ps.method = :method " +
           "ORDER BY ps.score DESC")
    List<PaperSimilarity> findBySourcePaperIdAndMethodOrderByScoreDesc(
            @Param("sourceId") Long sourceId,
            @Param("method") SimilarityMethod method);

    Optional<PaperSimilarity> findBySourcePaperIdAndTargetPaperIdAndMethod(
            Long sourcePaperId, Long targetPaperId, SimilarityMethod method);

    @Modifying
    @Query("DELETE FROM PaperSimilarity ps WHERE ps.sourcePaper.id = :paperId OR ps.targetPaper.id = :paperId")
    void deleteBySourcePaperIdOrTargetPaperId(@Param("paperId") Long paperId);
}
