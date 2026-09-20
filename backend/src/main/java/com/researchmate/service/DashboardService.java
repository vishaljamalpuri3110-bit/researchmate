package com.researchmate.service;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.researchmate.dto.response.DashboardResponse;
import com.researchmate.dto.response.DashboardResponse.ActivityDto;
import com.researchmate.dto.response.DashboardResponse.NotificationDto;
import com.researchmate.dto.response.PaperResponse;
import com.researchmate.entity.Paper;
import com.researchmate.entity.PaperStatus;
import com.researchmate.entity.User;
import com.researchmate.repository.NotificationRepository;
import com.researchmate.repository.PaperRepository;
import com.researchmate.repository.ResearchActivityRepository;
import com.researchmate.repository.ResearchGapRepository;
import com.researchmate.repository.UserRepository;
import com.researchmate.security.SecurityUtils;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final PaperRepository paperRepository;
    private final ResearchGapRepository gapRepository;
    private final NotificationRepository notificationRepository;
    private final ResearchActivityRepository activityRepository;
    private final UserRepository userRepository;

    public DashboardService(
            PaperRepository paperRepository,
            ResearchGapRepository gapRepository,
            NotificationRepository notificationRepository,
            ResearchActivityRepository activityRepository,
            UserRepository userRepository) {
        this.paperRepository = paperRepository;
        this.gapRepository = gapRepository;
        this.notificationRepository = notificationRepository;
        this.activityRepository = activityRepository;
        this.userRepository = userRepository;
    }

    public DashboardResponse getDashboardMetrics() {
        User currentUser = getCurrentUser();
        Long userId = currentUser.getId();

        long totalPapers = paperRepository.countByOwnerId(userId);
        long analyzedPapers = paperRepository.countByOwnerIdAndStatus(userId, PaperStatus.ANALYZED);
        long candidateGapsCount = gapRepository.findByOwnerIdWithEvidence(userId).size();
        long unreadNotifications = notificationRepository.countByUserIdAndReadFalse(userId);

        List<PaperResponse> recentPapers = paperRepository
                .findByOwnerId(userId, PageRequest.of(0, 5, Sort.by("createdAt").descending()))
                .getContent()
                .stream()
                .map(this::toPaperResponse)
                .toList();

        List<ActivityDto> recentActivities = activityRepository
                .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 8))
                .stream()
                .map(a -> new ActivityDto(a.getId(), a.getActivityType(), a.getDescription(), a.getCreatedAt()))
                .toList();

        List<NotificationDto> notifications = notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .limit(5)
                .map(n -> new NotificationDto(n.getId(), n.getMessage(), n.isRead(), n.getCreatedAt()))
                .toList();

        return new DashboardResponse(
                totalPapers,
                analyzedPapers,
                candidateGapsCount,
                unreadNotifications,
                recentPapers,
                recentActivities,
                notifications
        );
    }

    private PaperResponse toPaperResponse(Paper paper) {
        boolean hasText = paper.getExtractedText() != null && !paper.getExtractedText().isBlank();
        return new PaperResponse(
                paper.getId(),
                paper.getTitle(),
                paper.getAbstractText(),
                paper.getPublicationYear(),
                paper.getStatus(),
                paper.getOriginalFileName(),
                hasText,
                paper.getCreatedAt(),
                paper.getUpdatedAt()
        );
    }

    private User getCurrentUser() {
        String email = SecurityUtils.getCurrentUserEmail();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
    }
}
