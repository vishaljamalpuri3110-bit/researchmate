package com.researchmate.entity;

import java.time.Instant;

import jakarta.persistence.*;

@Entity
@Table(name = "papers", indexes = {
    @Index(name = "idx_paper_owner", columnList = "user_id"),
    @Index(name = "idx_paper_pub_year", columnList = "publicationYear"),
    @Index(name = "idx_paper_status", columnList = "status")
})
public class Paper {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String abstractText;

    @Column(nullable = false)
    private Integer publicationYear;

    @Column(nullable = false)
    private String pdfPath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaperStatus status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Column(nullable = false)
    private String originalFileName;

    @Column(nullable = false, unique = true)
    private String storedFileName;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String extractedText;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User owner;

    public Paper() {
    }

    public User getOwner() {
        return owner;
    }
    public void setOwner(User owner) {
        this.owner = owner;
    }
    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;

        if (status == null) {
            status = PaperStatus.UPLOADED;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAbstractText() {
        return abstractText;
    }

    public void setAbstractText(String abstractText) {
        this.abstractText = abstractText;
    }

    public Integer getPublicationYear() {
        return publicationYear;
    }

    public void setPublicationYear(Integer publicationYear) {
        this.publicationYear = publicationYear;
    }

    public String getPdfPath() {
        return pdfPath;
    }

    public void setPdfPath(String pdfPath) {
        this.pdfPath = pdfPath;
    }

    public PaperStatus getStatus() {
        return status;
    }

    public void setStatus(PaperStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

public void setOriginalFileName(String originalFileName) {
    this.originalFileName = originalFileName;
}

public String getStoredFileName() {
    return storedFileName;
}

public void setStoredFileName(String storedFileName) {
    this.storedFileName = storedFileName;
}

public String getExtractedText() {
    return extractedText;
}

public void setExtractedText(String extractedText) {
    this.extractedText = extractedText;
}

}