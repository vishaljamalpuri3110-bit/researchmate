package com.researchmate.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "research_gaps", indexes = {
    @Index(name = "idx_gap_owner", columnList = "user_id")
})
public class ResearchGap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(nullable = false)
    private Double confidence;

    @Column(nullable = false)
    private Integer frequency;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User owner;

    @OneToMany(mappedBy = "researchGap", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ResearchGapEvidence> evidenceList = new ArrayList<>();

    public ResearchGap() {
    }

    public ResearchGap(String title, String description, Double confidence, Integer frequency, User owner) {
        this.title = title;
        this.description = description;
        this.confidence = confidence;
        this.frequency = frequency;
        this.owner = owner;
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public Integer getFrequency() {
        return frequency;
    }

    public void setFrequency(Integer frequency) {
        this.frequency = frequency;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public List<ResearchGapEvidence> getEvidenceList() {
        return evidenceList;
    }

    public void setEvidenceList(List<ResearchGapEvidence> evidenceList) {
        this.evidenceList = evidenceList;
    }

    public void addEvidence(ResearchGapEvidence evidence) {
        evidenceList.add(evidence);
        evidence.setResearchGap(this);
    }
}
