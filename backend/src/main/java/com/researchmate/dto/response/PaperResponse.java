package com.researchmate.dto.response;

import java.time.Instant;
import com.researchmate.entity.PaperStatus;

public class PaperResponse {

    private Long id;
    private String title;
    private String abstractText;
    private Integer publicationYear;
    private PaperStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    public PaperResponse(
            Long id,
            String title,
            String abstractText,
            Integer publicationYear,
            PaperStatus status,
            Instant createdAt,
            Instant updatedAt) {

        this.id = id;
        this.title = title;
        this.abstractText = abstractText;
        this.publicationYear = publicationYear;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getAbstractText() {
        return abstractText;
    }

    public Integer getPublicationYear() {
        return publicationYear;
    }

    public PaperStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}