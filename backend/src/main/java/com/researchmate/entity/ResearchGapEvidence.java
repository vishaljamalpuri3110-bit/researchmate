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
@Table(name = "research_gap_evidences", indexes = {
    @Index(name = "idx_evidence_gap", columnList = "research_gap_id"),
    @Index(name = "idx_evidence_paper", columnList = "paper_id")
})
public class ResearchGapEvidence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "research_gap_id", nullable = false)
    private ResearchGap researchGap;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paper_id", nullable = false)
    private Paper paper;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String evidenceText;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EvidenceSourceType sourceType;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public ResearchGapEvidence() {
    }

    public ResearchGapEvidence(ResearchGap researchGap, Paper paper, String evidenceText, EvidenceSourceType sourceType) {
        this.researchGap = researchGap;
        this.paper = paper;
        this.evidenceText = evidenceText;
        this.sourceType = sourceType;
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

    public ResearchGap getResearchGap() {
        return researchGap;
    }

    public void setResearchGap(ResearchGap researchGap) {
        this.researchGap = researchGap;
    }

    public Paper getPaper() {
        return paper;
    }

    public void setPaper(Paper paper) {
        this.paper = paper;
    }

    public String getEvidenceText() {
        return evidenceText;
    }

    public void setEvidenceText(String evidenceText) {
        this.evidenceText = evidenceText;
    }

    public EvidenceSourceType getSourceType() {
        return sourceType;
    }

    public void setSourceType(EvidenceSourceType sourceType) {
        this.sourceType = sourceType;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
