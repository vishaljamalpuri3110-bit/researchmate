package com.researchmate.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "paper_similarities", indexes = {
    @Index(name = "idx_sim_source", columnList = "source_paper_id"),
    @Index(name = "idx_sim_target", columnList = "target_paper_id"),
    @Index(name = "idx_sim_method", columnList = "method")
})
public class PaperSimilarity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_paper_id", nullable = false)
    private Paper sourcePaper;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_paper_id", nullable = false)
    private Paper targetPaper;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SimilarityMethod method;

    @Column(nullable = false)
    private Double score;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public PaperSimilarity() {
    }

    public PaperSimilarity(Paper sourcePaper, Paper targetPaper, SimilarityMethod method, Double score) {
        this.sourcePaper = sourcePaper;
        this.targetPaper = targetPaper;
        this.method = method;
        this.score = score;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Paper getSourcePaper() {
        return sourcePaper;
    }

    public void setSourcePaper(Paper sourcePaper) {
        this.sourcePaper = sourcePaper;
    }

    public Paper getTargetPaper() {
        return targetPaper;
    }

    public void setTargetPaper(Paper targetPaper) {
        this.targetPaper = targetPaper;
    }

    public SimilarityMethod getMethod() {
        return method;
    }

    public void setMethod(SimilarityMethod method) {
        this.method = method;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
