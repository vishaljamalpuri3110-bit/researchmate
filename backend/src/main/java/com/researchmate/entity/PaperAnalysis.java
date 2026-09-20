package com.researchmate.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "paper_analyses", indexes = {
    @Index(name = "idx_analysis_paper", columnList = "paper_id")
})
public class PaperAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paper_id", nullable = false, unique = true)
    private Paper paper;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String authors;

    @Column(columnDefinition = "TEXT")
    private String abstractText;

    @Column(columnDefinition = "TEXT")
    private String keywords;

    @Column(columnDefinition = "TEXT")
    private String researchProblem;

    @Column(columnDefinition = "TEXT")
    private String methodology;

    @Column(columnDefinition = "TEXT")
    private String dataset;

    @Column(columnDefinition = "TEXT")
    private String algorithms;

    @Column(columnDefinition = "TEXT")
    private String results;

    @Column(columnDefinition = "TEXT")
    private String limitations;

    @Column(columnDefinition = "TEXT")
    private String futureWork;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(nullable = false)
    private String analysisStatus;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public PaperAnalysis() {
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (analysisStatus == null) {
            analysisStatus = "COMPLETED";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Paper getPaper() {
        return paper;
    }

    public void setPaper(Paper paper) {
        this.paper = paper;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthors() {
        return authors;
    }

    public void setAuthors(String authors) {
        this.authors = authors;
    }

    public String getAbstractText() {
        return abstractText;
    }

    public void setAbstractText(String abstractText) {
        this.abstractText = abstractText;
    }

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public String getResearchProblem() {
        return researchProblem;
    }

    public void setResearchProblem(String researchProblem) {
        this.researchProblem = researchProblem;
    }

    public String getMethodology() {
        return methodology;
    }

    public void setMethodology(String methodology) {
        this.methodology = methodology;
    }

    public String getDataset() {
        return dataset;
    }

    public void setDataset(String dataset) {
        this.dataset = dataset;
    }

    public String getAlgorithms() {
        return algorithms;
    }

    public void setAlgorithms(String algorithms) {
        this.algorithms = algorithms;
    }

    public String getResults() {
        return results;
    }

    public void setResults(String results) {
        this.results = results;
    }

    public String getLimitations() {
        return limitations;
    }

    public void setLimitations(String limitations) {
        this.limitations = limitations;
    }

    public String getFutureWork() {
        return futureWork;
    }

    public void setFutureWork(String futureWork) {
        this.futureWork = futureWork;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getAnalysisStatus() {
        return analysisStatus;
    }

    public void setAnalysisStatus(String analysisStatus) {
        this.analysisStatus = analysisStatus;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
