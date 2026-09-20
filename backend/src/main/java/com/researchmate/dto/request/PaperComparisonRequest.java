package com.researchmate.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public class PaperComparisonRequest {

    @NotEmpty(message = "Paper IDs are required")
    @Size(min = 2, max = 5, message = "Comparison requires between 2 and 5 papers")
    private List<Long> paperIds;

    public PaperComparisonRequest() {
    }

    public PaperComparisonRequest(List<Long> paperIds) {
        this.paperIds = paperIds;
    }

    public List<Long> getPaperIds() {
        return paperIds;
    }

    public void setPaperIds(List<Long> paperIds) {
        this.paperIds = paperIds;
    }
}
