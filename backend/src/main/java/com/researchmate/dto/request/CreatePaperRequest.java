package com.researchmate.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreatePaperRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String abstractText;

    @NotNull(message = "Publication year is required")
    @Min(value = 1900, message = "Publication year must be valid")
    @Max(value = 2100, message = "Publication year must be valid")
    private Integer publicationYear;

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
}