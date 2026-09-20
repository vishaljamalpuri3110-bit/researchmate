package com.researchmate.dto.response;

import java.time.Instant;
import java.util.List;

public record DashboardResponse(
        long totalPapers,
        long analyzedPapers,
        long candidateGapsCount,
        long unreadNotifications,
        List<PaperResponse> recentPapers,
        List<ActivityDto> recentActivities,
        List<NotificationDto> notifications
) {
    public record ActivityDto(
            Long id,
            String activityType,
            String description,
            Instant createdAt
    ) {}

    public record NotificationDto(
            Long id,
            String message,
            boolean isRead,
            Instant createdAt
    ) {}
}
